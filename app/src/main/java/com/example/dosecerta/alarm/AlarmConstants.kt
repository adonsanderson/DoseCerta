package com.example.dosecerta.alarm

object AlarmConstants {
    const val CHANNEL_ID = "dose_reminders_channel"

    const val ACTION_DOSE_ALARM = "com.example.dosecerta.action.DOSE_ALARM"
    const val ACTION_MARK_DOSE_TAKEN = "com.example.dosecerta.action.MARK_DOSE_TAKEN"

    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_TREATMENT_ID = "extra_treatment_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_SCHEDULED_TIME_MILLIS = "extra_scheduled_time_millis"

    /**
     * Derives a deterministic, positive PendingIntent request code from the persisted Room doseId
     * to prevent collisions across medications (RN05).
     */
    fun requestCodeForDose(doseId: Long): Int {
        val mixed = (doseId xor (doseId ushr 32)).toInt() and 0x3FFFFFFF
        return if (mixed == 0) 1 else mixed
    }

    fun actionRequestCodeForDose(doseId: Long): Int =
        requestCodeForDose(doseId) or 0x40000000

    fun notificationIdForDose(doseId: Long): Int =
        requestCodeForDose(doseId)
}
