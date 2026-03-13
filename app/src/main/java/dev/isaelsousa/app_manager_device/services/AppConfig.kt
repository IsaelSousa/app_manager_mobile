package dev.isaelsousa.app_manager_device.services

import android.content.Context
import androidx.core.content.edit

object AppConfig {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_IP = "backend_ip"

    fun saveIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_IP, ip) }
    }

    fun getIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IP, "localhost") ?: "localhost"
    }
}