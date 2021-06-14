package com.wayfarer.boosterupgrade.cards

import com.wayfarer.boosterupgrade.cards.CardLayout.*
import java.math.BigDecimal

enum class CardType(val humanReadableName: String) {
    LAND("Land"),
    CREATURE("Creature"),
    ENCHANTMENT("Enchantment"),
    INSTANT("Instant"),
    SORCERY("Sorcery"),
    ARTIFACT("Artifact"),
    TRIBAL("Tribal"),
    PLANE("Plane"),
    HERO("Hero"),
    OTHER("Other"),
    SCHEME("Scheme"),
    CONSPIRACY("Conspiracy"),
    PLANESWALKER("Planeswalker"),
    PHENOMENON("Phenomenon"),
    EMBLEM("Emblem"),
    CARD("Card"),
    VANGUARD("Vanguard"),
    TOKEN("Token");

    companion object {
        fun byHumanReadableName(name: String): CardType =
            byHumanReadableNameOptional(name) ?: throw UnsupportedOperationException("Unsupported card type: $name")

        fun byHumanReadableNameOptional(name: String): CardType? = when (name) {
            "Scariest", "You’ll", "Ever", "See" -> OTHER
            "instant" -> INSTANT
            "Eaturecray" -> CREATURE
            "Summon" -> CREATURE
            else -> values().singleOrNull { it.humanReadableName == name }
        }
    }
}

enum class CardColor(val letter: String, val displayName: String) {
    GREEN("G", "Green"),
    BLACK("B", "Black"),
    WHITE("W", "White"),
    BLUE("U", "Blue"),
    RED("R", "Red");

    companion object {
        fun byLetter(letter: String): CardColor = values().single { it.letter == letter }
        fun byDisplayName(displayName: String): CardColor = values().single { it.displayName == displayName }
    }
}

enum class SuperType(val humanReadableName: String) {
    LEGENDARY("Legendary"),
    BASIC("Basic"),
    WORLD("World"),
    SNOW("Snow"),
    ONGOING("Ongoing"),
    HOST("Host");

    companion object {
        fun byHumanReadableNameOptional(name: String): SuperType? =
            values().singleOrNull { it.humanReadableName == name }

        fun byHumanReadableName(name: String): SuperType =
            byHumanReadableNameOptional(name)
                ?: throw UnsupportedOperationException("Unsupported super type: $name")
    }
}


data class MagicCardFace(
    val name: String,
    val types: Set<CardType>,
    val colorIdentity: Set<CardColor>,
    val colors: Set<CardColor>,
    val cost: String?,
    val text: String,
    val typeText: String,
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    val cmc: Int,
    val superTypes: Set<SuperType>,
    val subTypes: Set<String>,
    val side: String,
    val leadershipSkills: Set<String>,
    val isPlayable: Boolean,
    val hasNoDeckRestriction: Boolean,
    val edhrecRank: Int?
)

val MagicCardFace.parsedCost get() = parseCost(cost ?: "")

enum class CardLayout(val databaseName: String) {
    NORMAL("normal"),
    AFTERMATH("aftermath"),
    SPLIT("split"),
    FLIP("flip"),
    LEVELER("leveler"),
    SAGA("saga"),
    TRANSFORM("transform"),
    ADVENTURE("adventure"),
    MELD("meld"),
    SCHEME("scheme"),
    PLANAR("planar"),
    VANGUARD("vanguard"),
    HOST("host"),
    AUGMENT("augment"),
    MODAL("modal_dfc");

    companion object {
        fun fromDatabaseName(databaseName: String): CardLayout {
            return values().singleOrNull { it.databaseName == databaseName }
                ?: throw IllegalArgumentException("Don't know layout called $databaseName")
        }
    }

}


enum class FormatLegality(val stateName: String) {
    LEGAL("Legal"),
    BANNED("Banned"),
    RESTRICTED("Restricted"),
    NOT_LEGAL("Not Legal");

    companion object {
        fun fromName(name: String): FormatLegality {
            return values().single { it.stateName == name }
        }
    }
}


enum class MtgFormat(
    val formatName: String,
) {
    COMMANDER(formatName = "commander")
}


data class MagicCard(
    val faces: List<MagicCardFace>,
    val id: Int,
    val layout: CardLayout,
    val edhRecRank: Int,
    val eurPrice: BigDecimal?,
    val usdPrice: BigDecimal?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MagicCard

        if (faces.size != other.faces.size) return false
        if (faces.map { it.name } != other.faces.map { it.name }) return false

        return true
    }

    override fun hashCode(): Int {
        return faces.joinToString(" // ") { it.name }.hashCode()
    }
}

val MagicCard.cmc
    get() = when (layout) {
        SAGA, LEVELER, NORMAL -> faces.single().cmc
        SPLIT, AFTERMATH -> faces.sumOf { it.cmc }
        MODAL, MELD, ADVENTURE, TRANSFORM, FLIP -> faces.first().cmc
        AUGMENT, HOST, VANGUARD, PLANAR, SCHEME -> faces.sumOf { it.cmc } //hope that's correct
    }

fun MagicCard.canBeCastWith(colors: Set<CardColor>?) =
    playableFaces.filter { !it.isLand }.any { it.canBeCastWith(colors) }

fun MagicCardFace.canBeCastWith(colors: Set<CardColor>?): Boolean = parsedCost.all { it.canBeCastWith(colors) }

private fun Cost.canBeCastWith(colors: Set<CardColor>?): Boolean {
    if (colors == null) return true
    return when (this) {
        WhiteCost -> colors.contains(CardColor.WHITE)
        GreenCost -> colors.contains(CardColor.GREEN)
        BlueCost -> colors.contains(CardColor.BLUE)
        BlackCost -> colors.contains(CardColor.BLACK)
        RedCost -> colors.contains(CardColor.RED)
        is CombinedCost -> cost1.canBeCastWith(colors) || cost2.canBeCastWith(colors)
        is HalfCost -> cost.canBeCastWith(colors)
        else -> true
    }
}

