package com.example.cropdisease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.cropdisease.camera.CameraActivity2
import com.synnapps.carouselview.CarouselView

class MainActivity : AppCompatActivity() {
    var imageViewProcessing: ImageView? = null

lateinit var carouselView: CarouselView
    var sampleImages = intArrayOf(
        R.drawable.apples,
        R.drawable.grapes,
        R.drawable.raspberry,
        R.drawable.tomato,
        R.drawable.strawberry
    )

    var cropClasses = arrayOf(
        "Apples",
        "Grapes",
        "Raspberry",
        "Tomato",
        "Strawberry"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        carouselView = findViewById(R.id.carouselView)
        carouselView.pageCount = cropClasses.size

        carouselView.setImageListener { position, imageView ->
            imageView.setImageResource(sampleImages[position])
        }


        imageViewProcessing = findViewById<View>(R.id.image) as ImageView

         findViewById<Button>(R.id.camera).setOnClickListener {
             startActivity(Intent(this, CameraActivity2::class.java))
        }




    }


}