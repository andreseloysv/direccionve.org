package com.direccionve.estasbien.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.direccionve.estasbien.MainActivity
import com.direccionve.estasbien.R

/**
 * Local notification helper — no Firebase, no external services.
 * Called by EarthquakeCheckWorker when a significant quake is detected.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "earthquake_alerts"
    private const val NOTIFICATION_ID = 1001

    fun showEarthquakeAlert(
        context: Context,
        magnitude: String,
        place: String,
        earthquakeId: String
    ) {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("earthquake_id", earthquakeId)
            putExtra("action", "alert")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "I'm safe" action
        val safeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("earthquake_id", earthquakeId)
            putExtra("action", "safe")
        }
        val safePending = PendingIntent.getActivity(
            context, 1, safeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Need help" action
        val helpIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("earthquake_id", earthquakeId)
            putExtra("action", "help")
        }
        val helpPending = PendingIntent.getActivity(
            context, 2, helpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title, magnitude))
            .setContentText(context.getString(R.string.notification_body))
            .setSubText(place)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .addAction(0, context.getString(R.string.alert_yes), safePending)
            .addAction(0, context.getString(R.string.alert_no), helpPending)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_earthquake),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_earthquake_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
