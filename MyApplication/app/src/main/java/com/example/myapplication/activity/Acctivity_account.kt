package com.example.myapplication.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.databinding.AcctivityAccountBinding
import com.example.myapplication.model.ApiClient
import com.example.myapplication.model.ApiResponse
import com.example.myapplication.model.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class acctivity_account : AppCompatActivity() {
    private lateinit var binding: AcctivityAccountBinding
    private lateinit var apiService: ApiService
    private lateinit var sharedPref: SharedPreferences
    private val changePasswordDialog: Dialog by lazy {
        Dialog(this, R.style.CustomDialogTheme).apply {
            setContentView(R.layout.change_password_layout)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AcctivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            finish()
        }
        binding.logout.setOnClickListener {
            val intent = Intent(this@acctivity_account, welcome_activity::class.java)
            startActivity(intent)
        }
        binding.exit.setOnClickListener {
            clearSession()
            finishAffinity()
            System.exit(0)
        }
        sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val retrofit = ApiClient.instance
        apiService = retrofit.create(ApiService::class.java)

        binding.changePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        val eff = AnimationUtils.loadAnimation(this, R.anim.editview_login)
        val eff1 = AnimationUtils.loadAnimation(this, R.anim.editview_login1)

        binding.head.startAnimation(eff)
        binding.body.startAnimation(eff1)
    }

    private fun showChangePasswordDialog() {
        val email = changePasswordDialog.findViewById<EditText>(R.id.email)
        val oldPassword = changePasswordDialog.findViewById<EditText>(R.id.oldPassword)
        val newPassword = changePasswordDialog.findViewById<EditText>(R.id.newPassword)
        val btnUpdate = changePasswordDialog.findViewById<Button>(R.id.btnUpdate)
        val useremail = sharedPref.getString("email", null)
        email.setText(useremail)

        btnUpdate.setOnClickListener {
            val inputOldPassword = oldPassword.text.toString()
            val inputNewPassword = newPassword.text.toString()

            if (inputOldPassword.isEmpty() || inputNewPassword.isEmpty()) {
                Toast.makeText(this@acctivity_account, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val call = apiService.changePassword(email.text.toString(), inputOldPassword, inputNewPassword)
            call.enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null) {
                                when {
                                    apiResponse.message.contains("Password updated successfully") -> {
                                        Toast.makeText(this@acctivity_account, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                                        changePasswordDialog.dismiss()
                                    }
                                    apiResponse.message.contains("Wrong old password") -> {
                                        Toast.makeText(this@acctivity_account, "Mật khẩu cũ không đúng !", Toast.LENGTH_SHORT).show()
                                    }
                                    apiResponse.message.contains("User not exists") -> {
                                        Toast.makeText(this@acctivity_account, "Không tồn tại người dùng !", Toast.LENGTH_SHORT).show()
                                    }
                                    apiResponse.message.contains("Internal server error") -> {
                                        Toast.makeText(this@acctivity_account, "Lỗi hệ thống !", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        Toast.makeText(this@acctivity_account, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this@acctivity_account, "Phản hồi không hợp lệ từ máy chủ", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@acctivity_account, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@acctivity_account, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show()
                        changePasswordDialog.dismiss()
                    }
                }
            })
        }

        val close = changePasswordDialog.findViewById<ImageView>(R.id.CloseImg)
        close.setOnClickListener {
            changePasswordDialog.dismiss()
        }

        changePasswordDialog.show()
    }


    private fun clearSession() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}
