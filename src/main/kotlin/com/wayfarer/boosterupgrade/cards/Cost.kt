package com.wayfarer.boosterupgrade.cards

import kotlin.math.min


sealed class Cost {
    abstract val cmc: Int
    abstract val prettyString: String
}


object WhiteCost : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = "W"
}


object GreenCost : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = "G"
}


object BlueCost : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = "U"
}


object BlackCost : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = "B"
}


object RedCost : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = "R"
}


data class TextCost(val text: String) : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = text

    override fun toString(): String = "TextCost($text)"
}


data class GenericCost(val number: Int) : Cost() {
    override val cmc: Int
        get() = number
    override val prettyString: String
        get() = "$number"

    override fun toString(): String = "Generic($number)"
}


object XCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = "X"
}


object YCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = "Y"
}


object ZCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = "Z"
}


object SnowCost : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}


object TapCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}


object ChaosCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}


object EnergyCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}


object ColorlessCost : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}


data class CombinedCost(val cost1: Cost, val cost2: Cost) : Cost() {
    override val cmc: Int
        get() = min(cost1.cmc, cost2.cmc)
    override val prettyString: String
        get() = "${cost1.prettyString}/${cost2.prettyString}"
}


data class HalfCost(val cost: Cost) : Cost() {
    override val cmc: Int
        get() = 1
    override val prettyString: String
        get() = "1/2"
}


object PhyrexianCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = TODO("Phyrexian Cost not implemented yet")
}


object UntapCost : Cost() {
    override val cmc: Int
        get() = 0

    override val prettyString: String
        get() = "UNTAP"
}


object SacrificeCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = "Sac ~"
}


object LifeCost : Cost() {
    override val cmc: Int
        get() = 0
    override val prettyString: String
        get() = "Pay life"
}

val List<Cost>.prettyString: String
    get() = this.joinToString("") { "{${it.prettyString}}" }

private fun costSymbolToCost(value: String): Cost {
    val generic = value.toIntOrNull()

    return if (generic != null)
        GenericCost(generic)
    else if (value.matches(Regex(".+/.+"))) {
        val costs = value.split("/")

        if (costs.size != 2) {
            throw RuntimeException("Weird cost: $costs")
        }

        CombinedCost(
            costSymbolToCost(costs[0]),
            costSymbolToCost(costs[1])
        )
    } else when (value) {
        "U" -> BlueCost
        "R" -> RedCost
        "G" -> GreenCost
        "B" -> BlackCost
        "W" -> WhiteCost
        "P" -> PhyrexianCost
        "X" -> XCost
        "Y" -> YCost
        "Z" -> ZCost
        "S" -> SnowCost
        "C" -> ColorlessCost
        "HW" -> HalfCost(WhiteCost)
        "T" -> TapCost
        "CHAOS" -> ChaosCost
        "E" -> EnergyCost
        "Q" -> UntapCost
        else -> throw UnsupportedOperationException("Can't parse $value")
    }
}


private val symbolPattern = Regex("\\{([^}]+)}")

fun parseCost(costString: String): List<Cost> {
    val cost = ArrayList<Cost>()

    for (match in symbolPattern.findAll(costString)) {
        val value = match.groupValues[1]
        cost += costSymbolToCost(value)
    }

    return cost
}

val Cost.colors: Set<CardColor>
    get() = when (this) {
        WhiteCost -> setOf(CardColor.WHITE)
        GreenCost -> setOf(CardColor.GREEN)
        BlueCost -> setOf(CardColor.BLUE)
        BlackCost -> setOf(CardColor.BLACK)
        RedCost -> setOf(CardColor.RED)
        LifeCost, UntapCost, SacrificeCost, PhyrexianCost, TapCost,
        ChaosCost, EnergyCost, ColorlessCost, SnowCost, XCost, YCost,
        ZCost, is GenericCost, is TextCost -> emptySet()
        is CombinedCost -> this.cost1.colors + this.cost2.colors
        is HalfCost -> this.cost.colors
    }

fun Cost.toColor(): CardColor? =
    when (this) {
        is RedCost -> CardColor.RED
        is BlueCost -> CardColor.BLUE
        is GreenCost -> CardColor.GREEN
        is BlackCost -> CardColor.BLACK
        is WhiteCost -> CardColor.WHITE
        is ColorlessCost -> null
        else -> throw IllegalArgumentException("$this is not a cost that's convertible")
    }
