package com.wayfarer.boosterupgrade.util

import java.time.Duration
import java.time.LocalDateTime

fun LocalDateTime.olderThan(age: Duration): Boolean {
    return LocalDateTime.now() > this + age
}
