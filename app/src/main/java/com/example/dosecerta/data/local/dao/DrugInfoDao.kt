package com.example.dosecerta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dosecerta.data.local.entity.DrugInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugInfoEntity>)

    @Query("SELECT * FROM drug_info_cache ORDER BY cachedAtMillis DESC")
    fun observeAllCached(): Flow<List<DrugInfoEntity>>

    @Query(
        "SELECT * FROM drug_info_cache " +
            "WHERE LOWER(brandName) LIKE '%' || LOWER(:query) || '%' " +
            "OR LOWER(genericName) LIKE '%' || LOWER(:query) || '%' " +
            "OR LOWER(searchQuery) LIKE '%' || LOWER(:query) || '%' " +
            "ORDER BY cachedAtMillis DESC"
    )
    suspend fun searchCached(query: String): List<DrugInfoEntity>

    @Query("SELECT * FROM drug_info_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DrugInfoEntity?
}
