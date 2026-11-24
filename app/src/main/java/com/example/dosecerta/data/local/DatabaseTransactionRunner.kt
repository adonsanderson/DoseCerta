package com.example.dosecerta.data.local

import androidx.room.withTransaction

interface DatabaseTransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner(
    private val database: DoseCertaDatabase
) : DatabaseTransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T =
        database.withTransaction { block() }
}
