package com.example.myapplication.activity

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.R
import com.example.myapplication.Utils
import com.example.myapplication.databinding.ActivitySubmitBinding
import com.example.myapplication.model.ApiClient
import com.example.myapplication.model.ApiResponse
import com.example.myapplication.model.ApiService
import com.example.myapplication.model.Exercise
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ActivitySubmit : AppCompatActivity() {
    private lateinit var binding: ActivitySubmitBinding
    private lateinit var apiService: ApiService
    private lateinit var sharedPref: SharedPreferences
    private var selectedDate: String = ""
    private lateinit var dayEndEditText: EditText
    private var selectedTime: String = ""
    private lateinit var timeEndEditText: EditText
    private val TAG = "ActivitySubmit"
    private val createEditExerciseDialog: Dialog by lazy {
        Dialog(this, R.style.CustomDialogTheme).apply {
            setContentView(R.layout.create_exercise_edit_dialog)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        // Kiểm tra kết nối internet
        if (!Utils.isOnline(this)) {
            showErrorDialog("Không có kết nối Internet. Vui lòng kiểm tra lại.")
            return
        }
        apiService = ApiClient.instance.create(ApiService::class.java)

        // Bắt đầu gửi yêu cầu API
        val postTitle = intent.getStringExtra("postTitle")
        val postId = intent.getStringExtra("postID")

        binding.exName.text = postTitle

        fetchPostData(postId.toString())

        binding.back.setOnClickListener {
            finish()
        }
        //cho phép copy link
        binding.exlinkdrive.setTextIsSelectable(true)
        //khi nhấp vào
        binding.exlinkdrive.movementMethod = LinkMovementMethod.getInstance()

        binding.button.setOnClickListener {
            // Lấy dữ liệu cần thiết từ giao diện

            val studentId =sharedPref.getInt("id", -1).toString()
            val submissionContent = binding.inpLink.text.toString().trim()

            // Kiểm tra nếu nội dung bài nộp không rỗng
            if (submissionContent.isNotEmpty()) {
                // Gọi hàm submitAssignment để gửi yêu cầu API
                submitAssignment(postId.toString(), studentId, submissionContent)
            } else {
                // Hiển thị thông báo nếu nội dung bài nộp trống
                Toast.makeText(this, "Vui lòng nhập nội dung bài nộp", Toast.LENGTH_SHORT).show()
            }
        }

        binding.editEx.setOnClickListener {
            val exTitle = createEditExerciseDialog.findViewById<EditText>(R.id.exTitle)
            val exContext = createEditExerciseDialog.findViewById<EditText>(R.id.exContext)
            val exLink = createEditExerciseDialog.findViewById<EditText>(R.id.exLink)
            val close = createEditExerciseDialog.findViewById<ImageView>(R.id.closeexCreate)

            dayEndEditText = createEditExerciseDialog.findViewById(R.id.dayEnd)
            timeEndEditText = createEditExerciseDialog.findViewById(R.id.timeEnd)
            val userId = sharedPref.getInt("id", -1).toString()
            val btn = createEditExerciseDialog.findViewById<Button>(R.id.btnEditex)
            close.setOnClickListener {
                createEditExerciseDialog.dismiss()
            }
            dayEndEditText.setOnClickListener{
                showDatePickerDialog()
            }

            exTitle.setText(postTitle)
            fetchPostDataToEdit(postId.toString(), exTitle, exContext, exLink, dayEndEditText, timeEndEditText)

            btn.setOnClickListener {
                val inpexTitle = exTitle.text.toString()
                val inpexContext = exContext.text.toString()
                val inpexLink = exLink.text.toString()
                val inpdayEnd = dayEndEditText.text.toString()
                val inptimeEnd = timeEndEditText.text.toString()
                if (inpexTitle.isNotEmpty() && inpexContext.isNotEmpty() && inpdayEnd.isNotEmpty() && inptimeEnd.isNotEmpty()) {
                    val call = apiService.editPost(
                        postId.toString(),
                        userId.toInt(),
                        inpexTitle,
                        inpexContext,
                        inpexLink,
                        "$inpdayEnd $inptimeEnd"
                    )
                    call.enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@ActivitySubmit,
                                    "Thay đổi thành công!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                createEditExerciseDialog.dismiss()
                                fetchPostData(postId.toString()) // Cập nhật lại dữ liệu bài tập sau khi chỉnh sửa
                            } else {
                                val responseBody = response.errorBody()?.string()
                                Log.e("activity_submit", "Phản hồi thất bại: ${response.message()} - ${responseBody}")
                                if (responseBody?.contains("You are not authorized to create a post for this class") == true) {
                                    Toast.makeText(
                                        this@ActivitySubmit,
                                        "Bạn không phải admin!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    createEditExerciseDialog.dismiss()
                                } else {
                                    Toast.makeText(
                                        this@ActivitySubmit,
                                        "Lỗi hệ thống!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    createEditExerciseDialog.dismiss()
                                }
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Log.e("activity_class", "Lỗi khi thay đổi: ${t.localizedMessage}")
                            Toast.makeText(
                                this@ActivitySubmit,
                                "Lỗi khi tạo bài tập!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(this@ActivitySubmit, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                }
            }

            createEditExerciseDialog.show()
        }
    }
    private fun submitAssignment(assignmentId: String, studentId: String, submissionContent: String) {
        // Gửi yêu cầu API để thêm bài nộp vào cơ sở dữ liệu
        val submitCall = apiService.submitAssignment(assignmentId, studentId, submissionContent)
        submitCall.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                // Xử lý kết quả thành công
                if (response == "Submission successful") {
                    // Hiển thị thông báo cho người dùng
                    Toast.makeText(this, "Nộp bài thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    // Hiển thị thông báo cho người dùng nếu có lỗi
                    Toast.makeText(this, "Đã xảy ra lỗi khi nộp bài", Toast.LENGTH_SHORT).show()
                }
            }, { error ->
                // Xử lý lỗi
                Log.e(TAG, "Error submitting assignment: ${error.message}")
                Toast.makeText(this, "Đã xảy ra lỗi khi nộp bài", Toast.LENGTH_SHORT).show()
            })
    }
    private fun fetchPostData(postId: String) {
        val call = apiService.getPostById(postId)
        call.enqueue(object : Callback<Exercise> {
            override fun onResponse(call: Call<Exercise>, response: Response<Exercise>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        val formattedDayEnd = result.dayEnd?.let { formatDateTime(it) }
                        binding.dayEnd.text = formattedDayEnd
                        binding.exName.text = result.postName
                        binding.excontext.text = result.postContent
                        binding.exlinkdrive.text = result.link_drive

                    } else {
                        showErrorDialog("Dữ liệu không tìm thấy. Vui lòng thử lại sau.")
                    }
                } else {
                    showErrorDialog("Lỗi máy chủ: ${response.code()}. Vui lòng thử lại sau.")
                }
            }

            override fun onFailure(call: Call<Exercise>, t: Throwable) {
                showErrorDialog("Lỗi kết nối: ${t.localizedMessage}. Vui lòng thử lại.")
            }
        })
    }

    private fun fetchPostDataToEdit(postId: String, exTitle: EditText, exContext: EditText, exLink: EditText, dayEnd: EditText, timeEnd: EditText) {
        val call = apiService.getPostById(postId)
        call.enqueue(object : Callback<Exercise> {
            override fun onResponse(call: Call<Exercise>, response: Response<Exercise>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        exTitle.setText(result.postName)
                        exContext.setText(result.postContent)
                        exLink.setText(result.link_drive)
                        val dateTimeParts = result.dayEnd?.split(" ")
                        if (dateTimeParts?.size == 2) {
                            dayEnd.setText(dateTimeParts[0])
                            timeEnd.setText(dateTimeParts[1])
                        }
                    } else {
                        showErrorDialog("Dữ liệu không tìm thấy. Vui lòng thử lại sau.")
                    }
                } else {
                    showErrorDialog("Lỗi máy chủ: ${response.code()}. Vui lòng thử lại sau.")
                }
            }

            override fun onFailure(call: Call<Exercise>, t: Throwable) {
                showErrorDialog("Lỗi kết nối: ${t.localizedMessage}. Vui lòng thử lại.")
            }
        })
    }

    private fun showErrorDialog(message: String) {
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(this)
                .setTitle(application.getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton("Thử lại") { dialog, _ ->
                    dialog.dismiss()
                    fetchPostData(intent.getStringExtra("postID").toString())
                }
                .setNegativeButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .show()
        }
    }

    private fun formatDateTime(dateTime: String): String {
        // Định dạng đầu vào từ ISO 8601
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Định dạng đầu ra theo yêu cầu
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()

        // Chuyển đổi và định dạng lại chuỗi thời gian
        val date = inputFormat.parse(dateTime)
        return outputFormat.format(date)
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
        val second = calendar.get(Calendar.SECOND)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minuteOfHour ->
                // Định dạng giờ đã chọn thành định dạng HH:MM
                selectedTime = String.format("%02d:%02d:%02d", hourOfDay, minuteOfHour, second)
                // Hiển thị giờ đã chọn trên EditText
                timeEndEditText.setText(selectedTime)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }
}
