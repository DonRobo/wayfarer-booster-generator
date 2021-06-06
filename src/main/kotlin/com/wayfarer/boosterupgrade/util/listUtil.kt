package com.wayfarer.boosterupgrade.util

fun List<Double>.median(): Double? =
    if (isNotEmpty()) sorted().let { (it[it.size / 2] + it[(it.size - 1) / 2]) / 2 } else null
