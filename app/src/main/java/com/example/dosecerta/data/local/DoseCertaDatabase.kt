package com.example.dosecerta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.dosecerta.data.local.dao.DoseDao
import com.example.dosecerta.data.local.dao.DrugInfoDao
import com.example.dosecerta.data.local.dao.TreatmentDao
import com.example.dosecerta.data.local.entity.DoseEntity
import com.example.dosecerta.data.local.entity.DrugInfoEntity
import com.example.dosecerta.data.local.entity.TreatmentEntity

@Database(
    entities = [
        TreatmentEntity::class,
        DoseEntity::class,
        DrugInfoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DoseCertaDatabase : RoomDatabase() {
    abstract fun treatmentDao(): TreatmentDao
    abstract fun doseDao(): DoseDao
    abstract fun drugInfoDao(): DrugInfoDao

    companion object {
        const val DATABASE_NAME = "dose_certa.db"
    }
}
