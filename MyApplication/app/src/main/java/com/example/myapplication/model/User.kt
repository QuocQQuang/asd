package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class User(

    @SerializedName("id"                 ) var id                : Int?    = null,
    @SerializedName("unique_id"          ) var uniqueId          : String? = null,
    @SerializedName("username"           ) var username          : String? = null,
    @SerializedName("email"              ) var email             : String? = null,
    @SerializedName("encrypted_password" ) var encryptedPassword : String? = null,
    @SerializedName("salt"               ) var salt              : String? = null,
    @SerializedName("created_at"         ) var createdAt         : String? = null,
    @SerializedName("updated_at"         ) var updatedAt         : String? = null
){
    companion object {
        var id: Int? = null
    }
}

