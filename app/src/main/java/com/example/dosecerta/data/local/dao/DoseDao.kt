package com.example.dosecerta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dosecerta.data.local.entity.DoseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doses: List<DoseEntity>): List<Long>

    @Query("SELECT * FROM doses ORDER BY scheduledTimeMillis ASC")
    fun observeAll(): Flow<List<DoseEntity>>

    @Query("SELECT * FROM doses WHERE treatmentId = :treatmentId ORDER BY scheduledTimeMillis ASC")
    fun observeByTreatmentId(treatmentId: Long): Flow<List<DoseEntity>>

    @Query(
        "SELECT * FROM doses " +
            "WHERE scheduledTimeMillis >= :startMillis AND scheduledTimeMillis < :endMillis " +
            "ORDER BY scheduledTimeMillis ASC"
    )
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<DoseEntity>>

    @Query("SELECT * FROM doses WHERE id = :doseId LIMIT 1")
    suspend fun getById(doseId: Long): DoseEntity?

    @Query("SELECT * FROM doses WHERE treatmentId = :treatmentId ORDER BY scheduledTimeMillis ASC")
    suspend fun getByTreatmentId(treatmentId: Long): List<DoseEntity>

    @Query(
        "SELECT * FROM doses " +
            "WHERE status = :pendingStatus AND scheduledTimeMillis > :nowMillis " +
            "ORDER BY scheduledTimeMillis ASC"
    )
    suspend fun getFuturePending(pendingStatus: String, nowMillis: Long): List<DoseEntity>

    @Query("UPDATE doses SET status = :status, takenAtMillis = :takenAtMillis WHERE id = :doseId")
    suspend fun updateStatus(doseId: Long, status: String, takenAtMillis: Long?)
}
