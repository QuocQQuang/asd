package com.example.myapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.model.MyClass

class ClassAdapter(context: Context, private val resource: Int, private val items: List<MyClass>) :
    ArrayAdapter<MyClass>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val classItem = items[position]
        val classIdTextView = view.findViewById<TextView>(R.id.classID)
        val classTitleTextView = view.findViewById<TextView>(R.id.title)
        val classNameTextView = view.findViewById<TextView>(R.id.adminName)

        // Truncate admin_name to a maximum of 10 characters
        val maxNameLength = 10
        val name = classItem.admin_name ?: ""
        val truncatedName = if (name.length > maxNameLength) {
            name.substring(0, maxNameLength) + "..."
        } else {
            name
        }
        classNameTextView.text = truncatedName

        // Truncate title to a maximum of 10 characters
        val maxTitleLength = 10
        val title = classItem.title ?: ""
        val truncatedTitle = if (title.length > maxTitleLength) {
            title.substring(0, maxTitleLength) + "..."
        } else {
            title
        }
        classTitleTextView.text = truncatedTitle

        classIdTextView.text = "ID: ${classItem.id}"

        return view
    }
}
