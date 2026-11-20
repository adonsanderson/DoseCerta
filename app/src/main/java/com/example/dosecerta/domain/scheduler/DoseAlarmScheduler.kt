package com.example.dosecerta.domain.scheduler

import com.example.dosecerta.domain.model.Dose

interface DoseAlarmScheduler {
    fun scheduleDose(dose: Dose)
    fun scheduleDoses(doses: List<Dose>)
    fun cancelDose(doseId: Long)
    fun cancelDoses(doseIds: List<Long>)
}
