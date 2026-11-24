package com.example.dosecerta

import android.app.Application
import com.example.dosecerta.alarm.NotificationHelper
import com.example.dosecerta.di.alarmModule
import com.example.dosecerta.di.dataModule
import com.example.dosecerta.di.domainModule
import com.example.dosecerta.di.networkModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class DoseCertaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@DoseCertaApplication)
                modules(
                    networkModule,
                    dataModule,
                    alarmModule,
                    domainModule
                )
            }
        }
        get<NotificationHelper>().createNotificationChannel()
    }
}
