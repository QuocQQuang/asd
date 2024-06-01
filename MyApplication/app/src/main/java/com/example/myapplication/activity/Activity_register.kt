package com.example.myapplication.activity

import android.content.Intent
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.animation.AnimationUtils
import com.example.myapplication.databinding.ActivityRegisterBinding
import io.reactivex.disposables.CompositeDisposable
import android.widget.Toast
import com.example.myapplication.model.ApiClient
import com.example.myapplication.model.ApiService
import com.example.myapplication.R


class activity_register : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    var compositeDisposable = CompositeDisposable()
    lateinit var apiService: ApiService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val retrofit = ApiClient.instance
        apiService = retrofit.create(ApiService::class.java)

        val eff = AnimationUtils.loadAnimation( this, R.anim.editview_login)
        val eff1 = AnimationUtils.loadAnimation( this, R.anim.editview_login1)
        val eff2 = AnimationUtils.loadAnimation( this, R.anim.editview_login2)
        val eff3 = AnimationUtils.loadAnimation( this, R.anim.btn)



        binding.editTextTextPersonName.startAnimation(eff)
        binding.editTextTextPassword.startAnimation(eff1)
        binding.editTextTextEmailAddress.startAnimation(eff2)
        binding.button.startAnimation(eff3)
        // xu ly back
        binding.back.setOnClickListener{
            finish()
        }
        // xy ly khi nhan dang ky
        binding.button.setOnClickListener {
            register(binding.editTextTextEmailAddress.text.toString(),binding.editTextTextPersonName.text.toString(),binding.editTextTextPassword.text.toString())
        }

        // xy ly khi nhan dang nhap
        binding.singin.setOnClickListener {
            val intent = Intent(this@activity_register, activity_login::class.java)
            startActivity(intent)
        }
    }

    private fun register(email: String, name: String, password: String) {
        if (email.isEmpty()||name.isEmpty() || password.isEmpty()) {
            // Hiển thị thông báo lỗi khi email hoặc password bị trống
            Toast.makeText(this@activity_register, "Email, Tên và Mật khẩu không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Hiển thị thông báo lỗi khi định dạng email không hợp lệ
            Toast.makeText(this@activity_register, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }
        compositeDisposable.add(apiService.registerUser(email,name, password)
            .subscribeOn(Schedulers.io())
            .observeOn (AndroidSchedulers.mainThread() )
            .subscribe{message->
                    Toast.makeText(this@activity_register, message, Toast.LENGTH_SHORT).show()
                val intent = Intent( this@activity_register, activity_login::class.java)
                startActivity(intent)
            })
    }
    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

}
