package com.wayfarer.boosterupgrade.util

import java.util.*

fun String.withoutSections(sectionStart: String, sectionEnd: String): String {
    val stringBuilder = StringBuilder()

    var position = 0
    while (position < length) {
        val next = indexOf(sectionStart, startIndex = position)
        if (next == -1) return (stringBuilder.toString() + substring(position)).trim()

        stringBuilder.append(substring(position, next))
        position = indexOf(sectionEnd, startIndex = next) + 1
        if (position == -1) throw RuntimeException()
    }

    return stringBuilder.toString().trim()
}

val Number.asEuros: String
    get() = String.format(Locale.ENGLISH, "%.2f€", this.toDouble())
