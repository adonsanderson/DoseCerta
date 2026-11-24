package com.example.dosecerta.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext

class DoseAlarmReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.action != AlarmConstants.ACTION_DOSE_ALARM) {
            return
        }

        val doseId = intent.getLongExtra(AlarmConstants.EXTRA_DOSE_ID, -1L)
        val treatmentId = intent.getLongExtra(AlarmConstants.EXTRA_TREATMENT_ID, -1L)
        val medicationName = intent.getStringExtra(AlarmConstants.EXTRA_MEDICATION_NAME).orEmpty()
        val dosage = intent.getStringExtra(AlarmConstants.EXTRA_DOSAGE).orEmpty()
        val scheduledTimeMillis = intent.getLongExtra(
            AlarmConstants.EXTRA_SCHEDULED_TIME_MILLIS,
            System.currentTimeMillis()
        )

        if (doseId <= 0L || medicationName.isBlank()) return

        val notificationHelper = GlobalContext.getOrNull()?.get<NotificationHelper>()
            ?: NotificationHelper(context.applicationContext)

        notificationHelper.showDoseReminderNotification(
            doseId = doseId,
            treatmentId = treatmentId,
            medicationName = medicationName,
            dosage = dosage,
            scheduledTimeMillis = scheduledTimeMillis
        )
    }
}
