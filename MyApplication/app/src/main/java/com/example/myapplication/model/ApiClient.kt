package com.example.myapplication.model

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var ourInstance: Retrofit? = null
    val instance: Retrofit
        get() {
            if (ourInstance == null) {
                ourInstance = Retrofit.Builder()
                    .baseUrl("http://192.168.191.31:3000/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())//Thêm converter Gson
                    .build()
            }
            return ourInstance!!
        }
}



