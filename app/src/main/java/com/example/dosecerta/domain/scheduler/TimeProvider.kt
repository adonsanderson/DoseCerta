package com.example.dosecerta.domain.scheduler

fun interface TimeProvider {
    fun nowMillis(): Long
}
