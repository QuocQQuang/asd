package com.example.myapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.model.Exercise
import java.text.SimpleDateFormat
import java.util.*

class ExerciseAdapter(context: Context, private val resource: Int, private val items: List<Exercise>) :
    ArrayAdapter<Exercise>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val exerciseItem = items[position]
        val dayCreatedTextView = view.findViewById<TextView>(R.id.dayEnd)
        val postNameTextView = view.findViewById<TextView>(R.id.postName)
        val exIdTextView = view.findViewById<TextView>(R.id.exId)

        exIdTextView.text = exerciseItem.postId.toString()
        val formattedDayEnd = exerciseItem.dayEnd?.let { formatDateTime(it) }
        dayCreatedTextView.text = formattedDayEnd
        postNameTextView.text = exerciseItem.postName

        return view
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
}
