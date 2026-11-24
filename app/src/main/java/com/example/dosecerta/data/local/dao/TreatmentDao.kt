package com.example.dosecerta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dosecerta.data.local.entity.TreatmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreatmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: TreatmentEntity): Long

    @Query("SELECT * FROM treatments ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatments WHERE id = :treatmentId LIMIT 1")
    fun observeById(treatmentId: Long): Flow<TreatmentEntity?>

    @Query("DELETE FROM treatments WHERE id = :treatmentId")
    suspend fun deleteById(treatmentId: Long)
}
