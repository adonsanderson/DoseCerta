package com.example.dosecerta.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHelper(
    private val context: Context
) {
    private val vibrationPattern = longArrayOf(0L, 400L, 250L, 400L)

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            AlarmConstants.CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            vibrationPattern = this@NotificationHelper.vibrationPattern
            setSound(alarmSoundUri, audioAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun showDoseReminderNotification(
        doseId: Long,
        treatmentId: Long,
        medicationName: String,
        dosage: String,
        scheduledTimeMillis: Long
    ) {
        if (!hasNotificationPermission()) return

        createNotificationChannel()

        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(scheduledTimeMillis))

        val title = context.getString(R.string.notification_dose_title, medicationName)
        val body = if (dosage.isNotBlank()) {
            context.getString(R.string.notification_dose_body_with_dosage, dosage, formattedTime)
        } else {
            context.getString(R.string.notification_dose_body_no_dosage, formattedTime)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmConstants.EXTRA_TREATMENT_ID, treatmentId)
            putExtra(AlarmConstants.EXTRA_DOSE_ID, doseId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            AlarmConstants.requestCodeForDose(doseId),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markTakenIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = AlarmConstants.ACTION_MARK_DOSE_TAKEN
            putExtra(AlarmConstants.EXTRA_DOSE_ID, doseId)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmConstants.actionRequestCodeForDose(doseId),
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, AlarmConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dose_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmSoundUri)
            .setVibrate(vibrationPattern)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_dose_notification,
                context.getString(R.string.notification_action_mark_taken),
                markTakenPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(AlarmConstants.notificationIdForDose(doseId), notification)
        } catch (_: SecurityException) {
            // Permission may have been revoked between check and notify call.
        }
    }

    fun cancelDoseNotification(doseId: Long) {
        NotificationManagerCompat.from(context)
            .cancel(AlarmConstants.notificationIdForDose(doseId))
    }

    fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
}
