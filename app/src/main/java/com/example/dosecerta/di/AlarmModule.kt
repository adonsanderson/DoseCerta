package com.example.dosecerta.di

import com.example.dosecerta.alarm.AndroidDoseAlarmScheduler
import com.example.dosecerta.alarm.NotificationHelper
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val alarmModule = module {
    single { NotificationHelper(androidContext()) }
    single { AndroidDoseAlarmScheduler(androidContext(), get()) }
    single<DoseAlarmScheduler> { get<AndroidDoseAlarmScheduler>() }
}
