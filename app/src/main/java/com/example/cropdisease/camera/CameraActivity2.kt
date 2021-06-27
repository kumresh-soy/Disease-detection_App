package com.example.cropdisease.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cropdisease.R
import com.example.cropdisease.cameraResult.CameraResults
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Suppress("DEPRECATION")
class CameraActivity2 : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var bitmap: Bitmap

    private lateinit var layout: LinearLayout

    private val mGalleryRequestCode = 101
    private var closeBtn: ImageButton? = null
    private var openGallery: ImageButton? = null
    private var camera_capture_button: ImageButton? = null

    private var viewFinder: PreviewView? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_fragment)
        viewFinder = findViewById(R.id.viewFinderCamera)

        supportActionBar?.hide()
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
            window.statusBarColor = ContextCompat.getColor(this, R.color.black)//status bar or the time bar at the top
        }


        // Request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listener for take photo button
        openGallery = findViewById(R.id.select_gallery)
        camera_capture_button = findViewById(R.id.btn)
        closeBtn = findViewById(R.id.close)
        layout = findViewById(R.id.layout_camera)

        //close button for camera
        closeBtn?.setOnClickListener { finish() }

        //choose image from gallery
        openGallery?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, mGalleryRequestCode)
        }
        //Take image from camera
        camera_capture_button?.setOnClickListener { takePhoto() }
        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

    }
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(
            Runnable {

                val cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder().build()
                preview?.setSurfaceProvider(viewFinder?.createSurfaceProvider())
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector, preview, imageCapture, imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(this)
        )
    }

    private fun takePhoto() {
        val photoFile = File(outputDirectory, "CropDisease-${System.currentTimeMillis()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            output,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
//                    val msg = "Photo capture succeeded: $savedUri"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    passImageData(savedUri.toString())
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity2, "Faild", Toast.LENGTH_LONG).show()
                }

            })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onPause() {
        super.onPause()
        isOffline = true
    }

    override fun onResume() {
        super.onResume()
        isOffline = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "please accept permission", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == mGalleryRequestCode ) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data?.data

                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                val destination = File(
                    outputDirectory,
                    "CropDisease-${System.currentTimeMillis()}.jpg"
                )

                val fo: FileOutputStream
                try {
                    destination.createNewFile()
                    fo = FileOutputStream(destination)
                    fo.write(bytes.toByteArray())

                    fo.close()
                    passImageData(destination.toString())

//                    Toast.makeText(this, "$destination", Toast.LENGTH_SHORT).show()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            //back button pressed if gallery not selected or image
            onBackPressed()
        }
    }

    fun passImageData(img: String){
        val intent =  Intent(this, CameraResults::class.java)
        intent.putExtra(IMAGE_URI, img)
        startActivity(intent)
    }

    private fun isOnline(activity: AppCompatActivity): Boolean {
        val connectivityManager=activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo=connectivityManager.activeNetworkInfo
        return  networkInfo!=null && networkInfo.isConnected
    }

    companion object {
        const val IMAGE_URI = "imageFileData"
        var isOffline = false // prevent app crash when goes offline
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}