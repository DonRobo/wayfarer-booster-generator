package com.wayfarer.boosterupgrade.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

fun LocalDateTime.olderThan(age: Duration): Boolean {
    return LocalDateTime.now() > this + age
}

fun LocalDate.olderThanDays(days: Long): Boolean {
    return LocalDate.now() > this.plusDays(days)
}

private val isoRegex = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
fun parseIsoDate(date: String): LocalDate {
    val matched = isoRegex.matchEntire(date) ?: error("$date is not a valid ISO date")

    return LocalDate.of(
        matched.groupValues[1].toInt(),
        matched.groupValues[2].toInt(),
        matched.groupValues[3].toInt(),
    )
}
