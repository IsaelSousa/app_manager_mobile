package dev.isaelsousa.app_manager_device.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.isaelsousa.app_manager_device.R
import dev.isaelsousa.app_manager_device.activities.MainActivity
import dev.isaelsousa.app_manager_device.data.network.NetworkModule
import dev.isaelsousa.app_manager_device.data.remote.AppManagerApi
import dev.isaelsousa.app_manager_device.models.CheckUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    @SuppressLint("HardwareIds")
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val api = NetworkModule.getRetrofitInstance(AppConfig.getBaseUrl(applicationContext))
                    .create(AppManagerApi::class.java)

                val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

                val response = api.checkUpdate(device = androidId)

                if (response.status) {
                    showNotification(response.data)
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun showNotification(updates: List<CheckUpdate>?) {
        if (updates == null) return

        val context = applicationContext
        val channelId = "updates_channel"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Atualizações de Apps",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisa quando há novos APKs disponíveis"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val inboxStyle = NotificationCompat.InboxStyle()
        updates.take(5).forEach { app ->
            inboxStyle.addLine("${app.appName}: v${app.version}")
        }
        if (updates.size > 5) {
            inboxStyle.setSummaryText("+${updates.size - 5} outros aplicativos")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Atualizações disponíveis")
            .setContentText("${updates.size} apps possuem novas versões")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}