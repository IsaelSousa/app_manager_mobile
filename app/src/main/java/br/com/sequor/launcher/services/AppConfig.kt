package br.com.sequor.launcher.services

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object AppConfig {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_IP = "backend_ip"
    private const val KEY_PORT = "backend_port"

    fun saveIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_IP, ip) }
    }

    fun savePort(context: Context, port: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_PORT, port) }
    }

    fun getIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IP, "localhost") ?: "localhost"
    }

    fun getPort(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PORT, "8080") ?: "8080"
    }

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ip = prefs.getString(KEY_IP, "localhost") ?: "localhost"
        val port = prefs.getString(KEY_PORT, "8080") ?: "8080"
        return "$ip:$port"
    }

    suspend fun isReachable(ip: String, port: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port.toInt()), 2000)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}