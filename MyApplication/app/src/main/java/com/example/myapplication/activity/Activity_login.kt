package com.example.myapplication.activity

import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.animation.AnimationUtils
import com.example.myapplication.databinding.ActivityLoginBinding
import io.reactivex.disposables.CompositeDisposable
import android.widget.Toast
import com.example.myapplication.*
import com.example.myapplication.model.ApiClient
import com.example.myapplication.model.ApiService
import com.example.myapplication.model.User

class activity_login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var apiService: ApiService
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val retrofit = ApiClient.instance
        apiService = retrofit.create(ApiService::class.java)

        val eff = AnimationUtils.loadAnimation(this, R.anim.editview_login)
        val eff1 = AnimationUtils.loadAnimation(this, R.anim.editview_login1)
        val eff2 = AnimationUtils.loadAnimation(this, R.anim.btn)

        binding.editTextTextEmailAddress.startAnimation(eff)
        binding.editTextTextPassword.startAnimation(eff1)
        binding.button.startAnimation(eff2)

        // Xử lý khi nhấn back
        binding.back.setOnClickListener {
            finish()
        }

        // Xử lý khi nhấn đăng nhập
        binding.button.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString()
            val password = binding.editTextTextPassword.text.toString()
            login(email, password)
        }

        // Xử lý khi nhấn đăng ký
        binding.login.setOnClickListener {
            val intent = Intent(this@activity_login, activity_register::class.java)
            startActivity(intent)
        }
    }

    private fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            // Hiển thị thông báo lỗi khi email hoặc password bị trống
            Toast.makeText(this@activity_login, "Email và Password không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Hiển thị thông báo lỗi khi định dạng email không hợp lệ
            Toast.makeText(this@activity_login, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        // Nếu email và password hợp lệ, thực hiện đăng nhập
        compositeDisposable.add(apiService.loginUser(email, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ user ->
                // Xử lý dữ liệu khi đăng nhập thành công
                saveUserData(user)
                Toast.makeText(this@activity_login, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@activity_login, MainActivity::class.java)
                startActivity(intent)
                // Chuyển sang màn hình khác hoặc thực hiện hành động khác
            }, { error ->
                // Xử lý khi có lỗi xảy ra trong quá trình đăng nhập
                Toast.makeText(this@activity_login, error.message, Toast.LENGTH_SHORT).show()
            })
        )
    }


    private fun saveUserData(user: User) {
        // Lưu thông tin người dùng vào SharedPreferences
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt("id", user.id ?: -1)
        editor.putString("unique_id", user.uniqueId)
        editor.putString("username", user.username)
        editor.putString("email", user.email)
        editor.putString("encrypted_password", user.encryptedPassword)
        editor.putString("salt", user.salt)
        editor.putString("created_at", user.createdAt)
        editor.putString("updated_at", user.updatedAt)
        editor.apply()
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