package com.wayfarer.boosterupgrade.edhrec

data class EdhRecRecommendation(
    val card: String,
    val synergyScore: Int,
    val usageScore: Int
)
