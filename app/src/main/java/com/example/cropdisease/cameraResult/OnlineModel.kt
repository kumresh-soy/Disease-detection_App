package com.example.cropdisease.cameraResult

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.cropdisease.R
import com.example.cropdisease.camera.CameraActivity2
import com.example.cropdisease.result.ResultActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.HashMap

class OnlineModel : AppCompatActivity() {
    private var imageView: ImageView? = null
    private var cropLable: String? = ""
    private var bitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_model)

        imageView = findViewById(R.id.photo_view_pager)
        val predict = findViewById<ImageButton>(R.id.btn_predict)
        val extras = intent.extras
        val myUri = Uri.parse(extras!!.getString(CameraActivity2.IMAGE_URI))
        imageView!!.setImageURI(myUri)
        val bitmapData = imageToBitmap()
        val encodedData = convertToBase64(bitmapData)
        callAPI(encodedData!!)
        supportActionBar?.hide()
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
            window.statusBarColor =
                ContextCompat.getColor(this, R.color.black)//status bar or the time bar at the top
        }

        //predict button
        predict.setOnClickListener {

            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("crop",cropLable)
            startActivity(intent)

            finish()
        }
    }

    private fun imageToBitmap():Bitmap{
        val drawable = imageView!!.drawable as BitmapDrawable
        return drawable.bitmap
    }
    private fun convertToBase64(bitmap:Bitmap):String?{
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
        val image = stream.toByteArray()
        return Base64.encodeToString(image,Base64.DEFAULT)
    }

    private fun callAPI(encodeString: String) {
        val url = "https://predict-f2xawc2bfq-el.a.run.app/"
        val queue = Volley.newRequestQueue(this)

        val request: StringRequest = object : StringRequest(Request.Method.POST, url, Response.Listener { response ->
            try {
                // on below line we are extracting data from our json object
                // and passing our response to our json object.
                val jsonObject = JSONObject(response)

                // creating a string for our output.
                val output = """
                  ${jsonObject.getString("result")}
                  ${jsonObject.getString("score")}
                  """.trimIndent()

                cropLable = output
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }, Response.ErrorListener {
            // displaying toast message on response failure.
            Toast.makeText(this, "Failed to connect server", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String>? {
                val params: MutableMap<String, String> = HashMap()
                params["image"] = encodeString
                return params
            }
        }
        queue.add(request)
    }


}