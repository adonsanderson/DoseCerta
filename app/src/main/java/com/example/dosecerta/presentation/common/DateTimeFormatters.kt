package com.example.dosecerta.presentation.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeFormatters {
    private val ptBrLocale = Locale("pt", "BR")

    fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", ptBrLocale).format(Date(millis))

    fun formatDate(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", ptBrLocale).format(Date(millis))

    fun formatShortDateTime(millis: Long): String =
        SimpleDateFormat("dd MMM • HH:mm", ptBrLocale).format(Date(millis))

    fun formatFullDateHeader(millis: Long): String =
        SimpleDateFormat("EEEE, dd 'de' MMMM", ptBrLocale)
            .format(Date(millis))
            .replaceFirstChar { it.uppercase(ptBrLocale) }

    fun todayBoundsMillis(nowMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.timeInMillis
        return start to end
    }
}
