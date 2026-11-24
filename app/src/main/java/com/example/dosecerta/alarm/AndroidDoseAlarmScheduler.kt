package com.example.dosecerta.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler

class AndroidDoseAlarmScheduler(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) : DoseAlarmScheduler {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(AlarmManager::class.java)

    override fun scheduleDose(dose: Dose) {
        if (dose.id <= 0L) return
        val manager = alarmManager ?: return
        val pendingIntent = createAlarmPendingIntent(
            doseId = dose.id,
            treatmentId = dose.treatmentId,
            medicationName = dose.medicationName,
            dosage = dose.dosage,
            scheduledTimeMillis = dose.scheduledTimeMillis,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dose.scheduledTimeMillis,
                    pendingIntent
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dose.scheduledTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dose.scheduledTimeMillis,
                pendingIntent
            )
        }
    }

    override fun scheduleDoses(doses: List<Dose>) {
        doses.forEach { dose -> scheduleDose(dose) }
    }

    override fun cancelDose(doseId: Long) {
        if (doseId <= 0L) return
        val manager = alarmManager
        val existingPendingIntent = createAlarmPendingIntent(
            doseId = doseId,
            treatmentId = 0L,
            medicationName = "",
            dosage = "",
            scheduledTimeMillis = 0L,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingPendingIntent != null) {
            manager?.cancel(existingPendingIntent)
            existingPendingIntent.cancel()
        }
        notificationHelper.cancelDoseNotification(doseId)
    }

    override fun cancelDoses(doseIds: List<Long>) {
        doseIds.forEach { doseId -> cancelDose(doseId) }
    }

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun createExactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            null
        }

    private fun createAlarmPendingIntent(
        doseId: Long,
        treatmentId: Long,
        medicationName: String,
        dosage: String,
        scheduledTimeMillis: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_DOSE_ALARM
            putExtra(AlarmConstants.EXTRA_DOSE_ID, doseId)
            putExtra(AlarmConstants.EXTRA_TREATMENT_ID, treatmentId)
            putExtra(AlarmConstants.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(AlarmConstants.EXTRA_DOSAGE, dosage)
            putExtra(AlarmConstants.EXTRA_SCHEDULED_TIME_MILLIS, scheduledTimeMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            AlarmConstants.requestCodeForDose(doseId),
            intent,
            flags
        )
    }
}
