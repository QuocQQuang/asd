package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class Exercise(
    @SerializedName("post_id") val postId: Int? = null,
    @SerializedName("class_id") val classId: Int? = null,
    @SerializedName("author_id") val authorId: Int? = null,
    @SerializedName("post_name") val postName: String? = null,
    @SerializedName("post_content") val postContent: String? = null,
    @SerializedName("link_drive") val link_drive: String? = null,
    @SerializedName("day_created") val dayCreated: String? = null,
    @SerializedName("day_end") val dayEnd: String? = null
)
