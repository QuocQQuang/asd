package com.example.myapplication

import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.widget.LinearLayout

class Utils {
    companion object{
        fun isOnline(content: Context): Boolean{
            val manager = content.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = manager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected

        }
    }
}
fun Dialog.setupDialog(layoutResId: Int) {
    setContentView(layoutResId)
    window!!.setLayout(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    setCancelable(false)
}