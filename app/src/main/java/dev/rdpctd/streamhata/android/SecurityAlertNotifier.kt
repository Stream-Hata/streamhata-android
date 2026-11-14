package dev.rdpctd.streamhata.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.AuthManager
import dev.rdpctd.streamhata.core.SecurityAlertSummary
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SecurityAlertNotifier(
    private val context: Context,
    private val api: ApiClient,
    private val auth: AuthManager
) {
    private val prefs = context.getSharedPreferences("security_alerts", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var started = false

    fun start() {
        if (started) return
        started = true
        createChannel()
        executor.scheduleWithFixedDelay({ poll() }, 0, 20, TimeUnit.SECONDS)
    }

    fun stop() {
        executor.shutdownNow()
    }

    private fun poll() {
        if (auth.token().isNullOrBlank()) return
        api.securityAlerts { alerts ->
            for (alert in alerts) {
                if (prefs.getBoolean("seen_${alert.id}", false)) continue
                prefs.edit().putBoolean("seen_${alert.id}", true).apply()
                notify(alert)
            }
        }
    }

    private fun notify(alert: SecurityAlertSummary) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            alert.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Безопасность StreamHata")
            .setContentText("Инцидент #${alert.incidentId} ждёт проверки администратора")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(alert.id.toInt(), notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Безопасность",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Опасные файлы, код и попытки мошенничества"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "security_alerts"
    }
}
