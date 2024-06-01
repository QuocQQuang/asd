package com.example.myapplication.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.example.myapplication.R

class activity_splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val eff = AnimationUtils.loadAnimation( this, R.anim.text_splash)

        val slogan= findViewById(R.id.textViewSlogan) as TextView
        val appname= findViewById(R.id.textViewAppName) as TextView

        appname.startAnimation(eff)
        slogan.startAnimation(eff)
        supportActionBar?.hide()
        Handler().postDelayed({
            val intent= Intent( this@activity_splash, welcome_activity::class.java)
            startActivity(intent)
        }, 3000)

    }
}