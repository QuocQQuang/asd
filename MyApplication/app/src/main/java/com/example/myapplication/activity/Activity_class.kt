package com.example.myapplication.activity

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.*
import com.example.myapplication.adapter.ExerciseAdapter
import com.example.myapplication.databinding.ActivityClassBinding
import com.example.myapplication.model.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class activity_class : AppCompatActivity() {
    private lateinit var binding: ActivityClassBinding
    private lateinit var apiService: ApiService
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var sharedPref: SharedPreferences
    private var selectedDate: String = ""
    private lateinit var dayEndEditText: EditText
    private var selectedTime: String = ""
    private lateinit var timeEndEditText: EditText

    private val createExerciseDialog: Dialog by lazy {
        Dialog(this, R.style.CustomDialogTheme).apply {
            setContentView(R.layout.create_exercise_dialog)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        if (!Utils.isOnline(this)) {
            val dialog = AlertDialog.Builder(this)
                .setTitle(application.getString(R.string.app_name))
                .setMessage("Thiết bị của bạn không kết nối với Internet!")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .show()
        }

        val classTitle = intent.getStringExtra("classTitle")
        val classId = intent.getStringExtra("classId")
        val maxTitleLength = 10
        val truncatedClassTitle = if (classTitle?.length ?: 0 > maxTitleLength) {
            classTitle?.substring(0, maxTitleLength) + "..."
        } else {
            classTitle
        }
        binding.classTitle.text = truncatedClassTitle
        val retrofit = ApiClient.instance
        apiService = retrofit.create(ApiService::class.java)
        if (classId != null) {
            checkAssignments(classId)
        }
        fetchExercises(classId.toString())

        binding.createExercises.setOnClickListener {
            val exTitle = createExerciseDialog.findViewById<EditText>(R.id.exTitle)
            val exContext = createExerciseDialog.findViewById<EditText>(R.id.exContext)
            dayEndEditText = createExerciseDialog.findViewById(R.id.dayEnd)
            timeEndEditText = createExerciseDialog.findViewById(R.id.timeEnd)
            val exLink= createExerciseDialog.findViewById<EditText>(R.id.exLink)
            val create = createExerciseDialog.findViewById<Button>(R.id.btnCreateex)

            // Gán sự kiện click cho trường day_end để mở DatePickerDialog
            dayEndEditText.setOnClickListener{
                showDatePickerDialog()
            }

            create.setOnClickListener {
                val inpexTitle = exTitle.text.toString()
                val inpexContext = exContext.text.toString()
                val inpexLink = exLink.text.toString()
                val inpday_end = selectedDate
                val inpTime_end = selectedTime
                val userId = sharedPref.getInt("id", -1).toString()

                if(inpexTitle.isNotEmpty() && inpexContext.isNotEmpty() && inpday_end.isNotEmpty() && inpTime_end.isNotEmpty()) {
                    if (!classId.isNullOrEmpty() && !userId.isNullOrEmpty() && classId != "-1") {
                        val call = apiService.createPost(
                            classId.toInt(),
                            userId.toInt(),
                            inpexTitle,
                            inpexContext,
                            inpexLink,
                            "$inpday_end $inpTime_end"
                        )
                        call.enqueue(object : Callback<ApiResponse> {
                            override fun onResponse(
                                call: Call<ApiResponse>,
                                response: Response<ApiResponse>
                            ) {
                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        this@activity_class,
                                        "Bài tập đã được tạo thành công!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Tải lại danh sách bài tập sau khi đã tạo thành công
                                    createExerciseDialog.dismiss()
                                    fetchExercises(classId.toString())
                                } else {
                                    val responseBody = response.errorBody()?.string()
                                    if (responseBody?.contains("You are not authorized to create a post for this class") == true) {
                                        Toast.makeText(
                                            this@activity_class,
                                            "Bạn không phải admin!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        createExerciseDialog.dismiss()
                                    } else {
                                        Log.e(
                                            "activity_class",
                                            "Phản hồi thất bại: ${response.message()}"
                                        )
                                        Toast.makeText(
                                            this@activity_class,
                                            "Lỗi hệ thống!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        createExerciseDialog.dismiss()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                Log.e("activity_class", "Lỗi khi tạo bài tập: $t")
                                Toast.makeText(
                                    this@activity_class,
                                    "Lỗi khi tạo bài tập!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@activity_class,
                            "Vui lòng điền đầy đủ thông tin!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this@activity_class, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                }
            }


            val close = createExerciseDialog.findViewById<ImageView>(R.id.closeexCreate)
            close.setOnClickListener {
                createExerciseDialog.dismiss()
            }
            createExerciseDialog.show()
        }
        binding.back.setOnClickListener {
            finish()
        }
        // chức năng edit exercise/ click vào 1 bài tập
        binding.listExercise.setOnItemClickListener { _, view, position, _ ->
            val delImg = view.findViewById<ImageView>(R.id.delImg)
            delImg.setOnClickListener {
                delExercise(position)
            }

            val selectedExercise = exerciseAdapter.getItem(position)
            val intent = Intent(this@activity_class, ActivitySubmit::class.java)
            intent.putExtra("postID", selectedExercise?.postId.toString())
            intent.putExtra("postTitle", selectedExercise?.postName)
            startActivity(intent)
        }
    }

    private fun delExercise(position: Int) {
        val selectedExercise = exerciseAdapter.getItem(position)
        if (selectedExercise != null) {
            AlertDialog.Builder(this)
                .setTitle("Xóa bài")
                .setMessage("Bạn muốn xóa bài này(chỉ quản trị viên)?")
                .setCancelable(true)
                .setPositiveButton("Yes") { dialog, _ ->
                    val userId = sharedPref.getInt("id", -1)

                    val postId = selectedExercise.postId

                    if (postId != null) {
                        apiService.delPost(postId.toInt(), userId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object : DisposableObserver<ApiResponse>() {
                                override fun onNext(response: ApiResponse) {
                                    // Xử lý khi xóa bài thành công
                                    Toast.makeText(this@activity_class, "Xóa bài thành công!", Toast.LENGTH_SHORT).show()
                                    // Cập nhật danh sách bài tập sau khi xóa
                                    val classId = intent.getStringExtra("classId")
                                    fetchExercises(classId.toString())
                                }
                                override fun onError(e: Throwable) {
                                    // Xử lý khi có lỗi xảy ra
                                    if(e.localizedMessage=="HTTP 403 Forbidden"){
                                        Toast.makeText(this@activity_class, "Bạn phải là Giáo Viên của lớp này!.", Toast.LENGTH_SHORT).show()
                                    }
                                    Log.e("activity_class", "Lỗi khi xóa bài: ${e.localizedMessage}")
                                    Toast.makeText(this@activity_class, "Lỗi khi xóa bài: ${e.localizedMessage}. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                                }

                                override fun onComplete() {
                                    // Không cần thực hiện gì cả
                                }
                            })
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }



    // Hàm để hiển thị DatePickerDialog
    fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Format ngày được chọn vào định dạng YYYY-MM-DD
                selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)
                // Hiển thị ngày đã chọn trên EditText
                dayEndEditText.setText(selectedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }
    fun showTimePickerDialog(view: View) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minuteOfHour ->
                // Định dạng giờ đã chọn thành định dạng HH:MM
                selectedTime = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                // Hiển thị giờ đã chọn trên EditText
                timeEndEditText.setText(selectedTime)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }

    private fun fetchExercises(classId: String) {
        val call = apiService.getPostsInClass(classId)
        call.enqueue(object : Callback<List<Exercise>> {
            override fun onResponse(call: Call<List<Exercise>>, response: Response<List<Exercise>>) {
                if (response.isSuccessful) {
                    val exercises = response.body()
                    exercises?.let {
                        // Sử dụng ExerciseAdapter với dữ liệu mới
                        exerciseAdapter =
                            ExerciseAdapter(this@activity_class, R.layout.list_exercise_layout, it)
                        binding.listExercise.adapter = exerciseAdapter
                    }
                } else {
                    Log.e("MainActivity", "Lỗi khi hiển thị bài: ${response.message()}")
                    Toast.makeText(this@activity_class, "Hiện không có bài nào!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Exercise>>, t: Throwable) {
                Log.e("MainActivity", "Lỗi khi hiển thị bài: $t")
                Toast.makeText(this@activity_class, "Hiện không có bài nào!", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun checkAssignments(classId: String) {
        val call = apiService.checkPosts(classId)
        call.enqueue(object : Callback<ApiResponsecheckPosts> {
            override fun onResponse(call: Call<ApiResponsecheckPosts>, response: Response<ApiResponsecheckPosts>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        // Handle the response and update the UI accordingly

                        val approachingAssignments = apiResponse.approachingAssignments

                        // Show the message as a Toast

                        // Display approaching assignments in the UI
                        val assignmentsMessage = approachingAssignments.joinToString("\n")
                        if(assignmentsMessage.isNotEmpty()) {
                            AlertDialog.Builder(this@activity_class)
                                .setTitle("Bài tập đến hạn")
                                .setMessage(assignmentsMessage)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                } else {
                    Log.e("activity_class", "Error fetching assignments: ${response.message()}")
                    Toast.makeText(this@activity_class, "Không có bài tập nào đến hạn!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponsecheckPosts>, t: Throwable) {
                Log.e("activity_class", "Failure fetching assignments: $t")
                Toast.makeText(this@activity_class, "Lỗi khi tìm kiếm bài tập đến hạn!", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
