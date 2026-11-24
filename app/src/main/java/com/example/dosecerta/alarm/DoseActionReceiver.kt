package com.example.dosecerta.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import com.example.dosecerta.domain.scheduler.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext

class DoseActionReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmConstants.ACTION_MARK_DOSE_TAKEN) return

        val doseId = intent.getLongExtra(AlarmConstants.EXTRA_DOSE_ID, -1L)
        if (doseId <= 0L) return

        val koin = GlobalContext.getOrNull() ?: return
        val treatmentRepository = koin.get<TreatmentRepository>()
        val alarmScheduler = koin.get<DoseAlarmScheduler>()
        val timeProvider = koin.get<TimeProvider>()
        val notificationHelper = koin.get<NotificationHelper>()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dose = treatmentRepository.getDoseById(doseId)
                if (dose != null && dose.status == DoseStatus.PENDING) {
                    treatmentRepository.updateDoseStatus(
                        doseId = doseId,
                        status = DoseStatus.TAKEN,
                        takenAtMillis = timeProvider.nowMillis()
                    )
                    alarmScheduler.cancelDose(doseId)
                }
                notificationHelper.cancelDoseNotification(doseId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
