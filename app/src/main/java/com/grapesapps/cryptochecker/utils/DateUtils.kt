package com.grapesapps.cryptochecker.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

class DateUtils {
    companion object {
        @SuppressLint("SimpleDateFormat")
        fun convertToDate(time: Long): String {
            val date = Date(time)
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
            return format.format(date)
        }
    }
}
