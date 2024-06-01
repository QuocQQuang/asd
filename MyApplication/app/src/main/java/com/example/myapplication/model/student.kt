package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class student(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("class_id") var class_id: Int? = null,
    @SerializedName("student_id") var student_id: Int? = null
)
