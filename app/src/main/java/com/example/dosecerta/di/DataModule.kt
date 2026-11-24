package com.example.dosecerta.di

import androidx.room.Room
import com.example.dosecerta.data.local.DatabaseTransactionRunner
import com.example.dosecerta.data.local.DoseCertaDatabase
import com.example.dosecerta.data.local.RoomTransactionRunner
import com.example.dosecerta.data.repository.DrugInfoRepositoryImpl
import com.example.dosecerta.data.repository.TreatmentRepositoryImpl
import com.example.dosecerta.domain.repository.DrugInfoRepository
import com.example.dosecerta.domain.repository.TreatmentRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<DoseCertaDatabase> {
        Room.databaseBuilder(
            androidContext(),
            DoseCertaDatabase::class.java,
            DoseCertaDatabase.DATABASE_NAME
        ).build()
    }

    single<DatabaseTransactionRunner> { RoomTransactionRunner(get()) }

    single { get<DoseCertaDatabase>().treatmentDao() }
    single { get<DoseCertaDatabase>().doseDao() }
    single { get<DoseCertaDatabase>().drugInfoDao() }

    single<TreatmentRepository> { TreatmentRepositoryImpl(get(), get(), get()) }
    single<DrugInfoRepository> { DrugInfoRepositoryImpl(get(), get(), get()) }
}
