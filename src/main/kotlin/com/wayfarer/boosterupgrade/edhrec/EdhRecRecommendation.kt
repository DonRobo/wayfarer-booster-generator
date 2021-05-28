package com.wayfarer.boosterupgrade.edhrec

import com.wayfarer.boosterupgrade.cards.MagicCard

data class EdhRecRecommendation(
    val card: MagicCard,
    val synergyScore: Int,
    val usageScore: Int
)
