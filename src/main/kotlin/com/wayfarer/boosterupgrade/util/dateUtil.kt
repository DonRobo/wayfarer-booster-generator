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
