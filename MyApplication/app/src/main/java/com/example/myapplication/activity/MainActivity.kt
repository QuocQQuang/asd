package com.example.myapplication.activity

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.*
import com.example.myapplication.adapter.ClassAdapter
import com.example.myapplication.model.ApiClient
import com.example.myapplication.model.ApiResponse
import com.example.myapplication.model.ApiService
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.myapplication.model.MyClass

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var apiService: ApiService
    private lateinit var classAdapter: ClassAdapter
    private lateinit var sharedPref: SharedPreferences
    private val createClassDialog: Dialog by lazy {
        Dialog(this, R.style.CustomDialogTheme).apply {
            setContentView(R.layout.create_class_layout)
        }
    }

    private val joinClassDialog: Dialog by lazy {
        Dialog(this, R.style.CustomDialogTheme).apply {
            setContentView(R.layout.join_layout)
        }
    }

    private val classList: MutableList<MyClass> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)

        if (!Utils.isOnline(this)) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle(application.getString(R.string.app_name))
            dialog.setMessage("Thiết bị của bạn không kết nối với Internet!")
            dialog.setCancelable(false)
            dialog.setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
                finish()
            }
            dialog.show()
        }
// khi nhấn vào account
        binding.account.setOnClickListener {
            val intent = Intent(this@MainActivity, acctivity_account::class.java)
            startActivity(intent)
        }


        val retrofit = ApiClient.instance
        apiService = retrofit.create(ApiService::class.java)

        fetchAllClasses()

        // tạo lớp mới
        binding.createClass.setOnClickListener {
            val title = createClassDialog.findViewById<EditText>(R.id.edtTitle)
            val btnCreate = createClassDialog.findViewById<Button>(R.id.btnCreate)

            btnCreate.setOnClickListener {
                val titleInput = title.text.toString()
                if (titleInput.isNotEmpty()) {
                    val userId = sharedPref.getInt("id", -1).toString()
                    val userName = sharedPref.getString("username", "Không có tên").toString()
                    if (userId != "-1") {
                        lifecycleScope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    createClassSuspend(apiService.createClass(titleInput, userId, userName))
                                }
                                if (result == "Class created successfully") {
                                    Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                                    fetchAllClasses() // Refresh the list of classes
                                    createClassDialog.dismiss()
                                } else {
                                    Toast.makeText(this@MainActivity, "Lỗi: $result", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Lỗi khi tạo lớp: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Lỗi không tìm thấy Id người dùng", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Vui lòng đặt tên lớp", Toast.LENGTH_SHORT).show()
                }
            }
            val close = createClassDialog.findViewById<ImageView>(R.id.closeImg)
            close.setOnClickListener {
                createClassDialog.dismiss()
            }
            createClassDialog.show()
        }

        // tham gia lớp
        binding.joinClass.setOnClickListener {
            val classId = joinClassDialog.findViewById<EditText>(R.id.classId)
            val btnJoin = joinClassDialog.findViewById<Button>(R.id.btnJoin)

            btnJoin.setOnClickListener {
                val idInput = classId.text.toString()
                if (idInput.isNotEmpty()) {
                    val userId = sharedPref.getInt("id", -1).toString()
                    lifecycleScope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                joinClassSuspend(apiService.joinClass(idInput, userId))
                            }
                            if (result == "Tham gia thành công!") {
                                fetchAllClasses() // Refresh the list of classes
                                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Gặp lỗi khi tham gia: $e")
                            Toast.makeText(this@MainActivity, "Gặp lỗi khi tham gia", Toast.LENGTH_SHORT).show()
                        }
                    }
                    joinClassDialog.dismiss()
                } else {
                    Toast.makeText(this@MainActivity, "Vui lòng nhập Id lớp", Toast.LENGTH_SHORT).show()
                }
            }
            val close = joinClassDialog.findViewById<ImageView>(R.id.close)
            close.setOnClickListener {
                joinClassDialog.dismiss()
            }
            joinClassDialog.show()
        }
        // xử lý khi nhấn vào xóa/ khi nhấn vào 1 lớp
        binding.listClass.setOnItemClickListener { _, view, position, _ ->
            val outImg = view.findViewById<ImageView>(R.id.outClass)
            outImg.setOnClickListener {
                outClass(position)
            }
            val selectedClass = classAdapter.getItem(position)
            val intent = Intent(this@MainActivity, activity_class::class.java)
            intent.putExtra("classId", selectedClass?.id)
            intent.putExtra("classTitle", selectedClass?.title)
            startActivity(intent)
        }
    }

    private fun outClass(position: Int) {
        val selectedClass = classAdapter.getItem(position)
        if (selectedClass != null) {
            AlertDialog.Builder(this)
                .setTitle("Rời khỏi lớp")
                .setMessage("Bạn muốn rời khỏi lớp này?")
                .setCancelable(true)
                .setPositiveButton("Yes") { dialog, _ ->
                    lifecycleScope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                outClassSuspend(apiService.outClass(selectedClass.id.toString(), sharedPref.getInt("id", -1).toString()))
                            }
                            if (result.message == "Successfully left the class") {
                                classList.removeAt(position)
                                classAdapter.notifyDataSetChanged()
                                Toast.makeText(this@MainActivity, "Rời khỏi lớp thành công", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Lỗi: ${result.message}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
//                            Log.e("MainActivity", "Gặp lỗi khi rời khỏi lớp: $e")
//                            Toast.makeText(this@MainActivity, "Gặp lỗi khi rời khỏi lớp", Toast.LENGTH_SHORT).show()
                            classList.removeAt(position)
                            classAdapter.notifyDataSetChanged()
                            Toast.makeText(this@MainActivity, "Rời khỏi lớp thành công", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    private suspend fun outClassSuspend(observable: Observable<ApiResponse>): ApiResponse {
        return suspendCancellableCoroutine { cont ->
            val disposable = observable.subscribe(
                { response ->
                    cont.resume(response)
                },
                { error ->
                    cont.resumeWithException(error)
                }
            )
            cont.invokeOnCancellation {
                disposable.dispose()
            }
        }
    }


    private suspend fun joinClassSuspend(observable: Observable<String>): String {
        return suspendCancellableCoroutine { cont ->
            val disposable = observable.subscribe(
                { result ->
                    cont.resume(result)
                },
                { error ->
                    cont.resumeWithException(error)
                }
            )
            cont.invokeOnCancellation {
                disposable.dispose()
            }
        }
    }

    private fun fetchAllClasses() {
        val userId = sharedPref.getInt("id", -1).toString()
        lifecycleScope.launch {
            try {
                classList.clear()
                val userClassesResponse = withContext(Dispatchers.IO) {
                    apiService.getUserClasses(userId)
                }
                val adminClassesResponse = withContext(Dispatchers.IO) {
                    apiService.getUserAdminClasses(userId)
                }

                if (userClassesResponse.isSuccessful) {
                    val userClasses = userClassesResponse.body() ?: emptyList()
                    classList.addAll(userClasses)
                } else {
                    // Xử lý lỗi cho userClasses
                    val errorBody = userClassesResponse.errorBody()?.string()
                    if (errorBody?.contains("No classes found for this user") == true) {
                        Toast.makeText(this@MainActivity, "Bạn đang không tham gia lớp nào!", Toast.LENGTH_SHORT).show()
                    }else {
                        Log.e("MainActivity", "Error fetching user classes: $errorBody")
                        Toast.makeText(
                            this@MainActivity,
                            "Lỗi khi tải danh sách lớp học",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                if (adminClassesResponse.isSuccessful) {
                    val adminClasses = adminClassesResponse.body() ?: emptyList()
                    classList.addAll(adminClasses)
                } else {
                    // Xử lý lỗi cho adminClasses
                    val errorBody = adminClassesResponse.errorBody()?.string()
                    if (errorBody?.contains("No classes found for this admin") == true) {
                        Toast.makeText(this@MainActivity, "Hiện không có lớp nào của bạn", Toast.LENGTH_SHORT).show()
                    }else{
                        Log.e("MainActivity", "Error fetching admin classes: $errorBody")
                        Toast.makeText(this@MainActivity, "Lỗi khi tải danh sách lớp học", Toast.LENGTH_SHORT).show()
                    }
                }

                classAdapter = ClassAdapter(this@MainActivity, R.layout.list_class_layout, classList)
                binding.listClass.adapter = classAdapter
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching classes", e)
                Toast.makeText(this@MainActivity, "Lỗi khi tải danh sách lớp học", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun createClassSuspend(observable: Observable<ApiResponse>): String {
        return suspendCancellableCoroutine { cont ->
            val disposable = observable.subscribe(
                { response ->
                    cont.resume(response.message)
                },
                { error ->
                    cont.resumeWithException(error)
                }
            )
            cont.invokeOnCancellation {
                disposable.dispose()
            }
        }
    }

}