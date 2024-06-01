package com.example.myapplication.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.myapplication.databinding.ActivityWelcomeBinding


class welcome_activity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //chuyển qua activ khi nhấn đăng kí
        binding.button.setOnClickListener{
            val intent = Intent( this@welcome_activity, activity_register::class.java)
            startActivity(intent)
        }
        //chuyển qua act khi nhấn đăng nhập
        binding.signin.setOnClickListener{
            val intent = Intent( this@welcome_activity, activity_login::class.java)
            startActivity(intent)
        }
    }
}