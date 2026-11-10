package com.studysphere.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    fun today(): Long = LocalDate.now().toEpochDay()
    fun date(day: Long): LocalDate = LocalDate.ofEpochDay(day)
    fun fmt(day: Long, pattern: String = "EEE, d MMM"): String =
        date(day).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    fun time(min: Int): String = String.format(Locale.US, "%02d:%02d", min / 60, min % 60)
    fun nowMin(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
    fun dayOf(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    fun greeting(): String {
        val h = LocalTime.now().hour
        return when { h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
    }
    fun countdown(day: Long, today: Long = today()): String {
        val d = day - today
        return when {
            d < 0 -> "${-d} day${if (-d == 1L) "" else "s"} ago"
            d == 0L -> "Today"
            d == 1L -> "1 day remaining"
            else -> "$d days remaining"
        }
    }
    fun duration(min: Int): String = when {
        min >= 60 && min % 60 == 0 -> "${min / 60}h"
        min >= 60 -> "${min / 60}h ${min % 60}m"
        else -> "${min}m"
    }
    fun clock(sec: Int): String = String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60)
}
