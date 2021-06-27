package com.example.cropdisease.cameraResult

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.cropdisease.R
import com.example.cropdisease.camera.CameraActivity2.Companion.IMAGE_URI
import com.example.cropdisease.result.ResultActivity
import com.pixelcarrot.base64image.Base64Image
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*


@Suppress("DEPRECATION")
class CameraResults : AppCompatActivity() {
    private var tflite: Interpreter? = null
    private val tfliteModel: MappedByteBuffer? = null
    private var bitmap: Bitmap? = null
    private var outputProbabilityBuffer: TensorBuffer? = null
    private var probabilityProcessor: TensorProcessor? = null
    var imageuri: Uri? = null
    private var cropLable: String? = ""

    private val modelFilePath = "plantVilages.tflite"
    private val labelPath = "labels.txt"

    private var inputImageBuffer: TensorImage? = null
    private var imageSizeX = 0
    private var imageSizeY = 0
    private var labels: List<String>? = null

    lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_results)

        imageView = findViewById(R.id.photo_view_pager)
        val predict = findViewById<ImageButton>(R.id.btn_predict)
        val extras = intent.extras
        val myUri = Uri.parse(extras!!.getString(IMAGE_URI))
        imageView.setImageURI(myUri)
        val drawable = imageView.drawable as BitmapDrawable
        bitmap = drawable.toBitmap()

        supportActionBar?.hide()
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
            window.statusBarColor =
                ContextCompat.getColor(this, R.color.black)//status bar or the time bar at the top
        }


           try {
            this.tflite = Interpreter(loadFile(this)!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        predict.setOnClickListener {
//            classifyImage()
                classifyImage()
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("crop",cropLable)
                startActivity(intent)


            finish()
        }
    }


    private fun classifyImage(){
        val imageTensorIndex = 0
        val imageShape = tflite!!.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
        imageSizeY = imageShape[1]
        imageSizeX = imageShape[2]
        val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()
        val probabilityTensorIndex = 0
        val probabilityShape = tflite!!.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
        val probabilityDataType = tflite!!.getOutputTensor(probabilityTensorIndex).dataType()

        inputImageBuffer = TensorImage(imageDataType)
        outputProbabilityBuffer = TensorBuffer.createFixedSize(
            probabilityShape,
            probabilityDataType
        )

        probabilityProcessor = TensorProcessor.Builder().add(Companion.getPostprocessNormalizeOp()).build()
        inputImageBuffer = loadImage(bitmap!!)
        tflite!!.run(inputImageBuffer!!.buffer, outputProbabilityBuffer!!.buffer.rewind())
        showResult()
    }

    private fun loadImage(bitmap: Bitmap): TensorImage? {
        // Loads bitmap into a TensorImage.
        inputImageBuffer!!.load(bitmap)

        // Creates processor for the TensorImage.
        val cropSize = Math.min(bitmap.width, bitmap.height)
        val imageProcessor: ImageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(Companion.getPreprocessNormalizeOp())
            .build()
        return imageProcessor.process(inputImageBuffer)
    }

    @Throws(IOException::class)
    private fun loadFile(activity: Activity): MappedByteBuffer? {

        val fileDescriptor = activity.assets.openFd(modelFilePath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val standoffs = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, standoffs, declaredLength)
    }



    private fun showResult() {
        try {

            labels = FileUtil.loadLabels(this, labelPath)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        val labeledProbability: Map<String, Float> = TensorLabel(
            labels!!, probabilityProcessor!!.process(
                outputProbabilityBuffer
            )
        )
            .mapWithFloatValue
        val maxValueInMap = Collections.max(labeledProbability.values)
        for ((key, value) in labeledProbability) {
            if (value == maxValueInMap) {
                cropLable = key
                Toast.makeText(this, "$key, $value", Toast.LENGTH_LONG).show()

            }
        }
    }

    companion object{
        private const val IMAGE_MEAN = 0.0f
        private const val IMAGE_STD = 255.0f
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 255.0f

        private fun getPreprocessNormalizeOp(): TensorOperator? {
            return NormalizeOp(CameraResults.IMAGE_MEAN, CameraResults.IMAGE_STD)
        }

        private fun getPostprocessNormalizeOp(): TensorOperator? {
            return NormalizeOp(CameraResults.PROBABILITY_MEAN, CameraResults.PROBABILITY_STD)
        }
    }
}