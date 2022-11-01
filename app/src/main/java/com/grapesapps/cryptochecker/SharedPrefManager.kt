package com.grapesapps.cryptochecker


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


class SharedPrefManager(context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    companion object {
        const val CRYPTOCURRENCY = "cryptoCurrency"
    }

    fun saveCryptoCurrency(cryptoCurrency: String) {
        prefs.edit {
            putString(CRYPTOCURRENCY, cryptoCurrency)

        }
    }

    fun getCryptoCurrency(): String? {
        return prefs.getString(CRYPTOCURRENCY, null)
    }

    fun removeCryptoCurrency() {
        prefs.edit {
            remove(CRYPTOCURRENCY)
        }
    }
}