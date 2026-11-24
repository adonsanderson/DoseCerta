package com.example.dosecerta.alarm

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndroidDoseAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: AndroidDoseAlarmScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        val notificationHelper = NotificationHelper(context)
        scheduler = AndroidDoseAlarmScheduler(context, notificationHelper)
    }

    @Test
    fun `scheduleDose registers alarm in AlarmManager with expected extras and trigger time`() {
        val shadowAlarmManager = shadowOf(alarmManager)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        val triggerMillis = 1_800_000_000_000L
        val dose = Dose(
            id = 55L,
            treatmentId = 12L,
            medicationName = "Amoxicilina",
            dosage = "500mg",
            sequenceNumber = 1,
            scheduledTimeMillis = triggerMillis,
            status = DoseStatus.PENDING
        )

        scheduler.scheduleDose(dose)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
        val alarm = scheduledAlarms.first()
        assertEquals(triggerMillis, alarm.triggerAtTime)

        val intent = shadowOf(alarm.operation).savedIntent
        assertNotNull(intent)
        assertEquals(AlarmConstants.ACTION_DOSE_ALARM, intent.action)
        assertEquals(55L, intent.getLongExtra(AlarmConstants.EXTRA_DOSE_ID, -1L))
        assertEquals(12L, intent.getLongExtra(AlarmConstants.EXTRA_TREATMENT_ID, -1L))
        assertEquals("Amoxicilina", intent.getStringExtra(AlarmConstants.EXTRA_MEDICATION_NAME))
        assertEquals("500mg", intent.getStringExtra(AlarmConstants.EXTRA_DOSAGE))
    }

    @Test
    fun `scheduleDose falls back gracefully when exact alarm permission is denied`() {
        val shadowAlarmManager = shadowOf(alarmManager)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        val dose = Dose(
            id = 77L,
            treatmentId = 3L,
            medicationName = "Ibuprofeno",
            dosage = "400mg",
            sequenceNumber = 1,
            scheduledTimeMillis = 1_800_050_000_000L,
            status = DoseStatus.PENDING
        )

        scheduler.scheduleDose(dose)

        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `cancelDose removes previously scheduled alarm from AlarmManager`() {
        val shadowAlarmManager = shadowOf(alarmManager)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        val dose = Dose(
            id = 88L,
            treatmentId = 4L,
            medicationName = "Dipirona",
            dosage = "1g",
            sequenceNumber = 1,
            scheduledTimeMillis = 1_800_100_000_000L,
            status = DoseStatus.PENDING
        )

        scheduler.scheduleDose(dose)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        scheduler.cancelDose(88L)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }
}
