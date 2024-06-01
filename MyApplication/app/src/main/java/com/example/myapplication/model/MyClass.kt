package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class MyClass(
    @SerializedName("id"    ) var id    : String?    = null,
    @SerializedName("title" ) var title : String? = null,
    @SerializedName("admin" ) var admin : Int?    = null,
    @SerializedName("admin_name" ) var admin_name : String?    = null
)
