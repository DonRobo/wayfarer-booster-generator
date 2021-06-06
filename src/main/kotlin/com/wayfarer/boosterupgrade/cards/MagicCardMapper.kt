package com.wayfarer.boosterupgrade.cards

import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.wayfarer.boosterupgrade.cards.CardLayout.*
import com.wayfarer.boosterupgrade.jooq.Tables
import com.wayfarer.boosterupgrade.util.getOrNull
import com.wayfarer.boosterupgrade.util.jooq.MtgRecordMapper
import org.jooq.Record
import org.jooq.impl.DSL
import java.math.BigDecimal
import kotlin.math.roundToInt
import com.wayfarer.boosterupgrade.jooq.tables.AtomicCard as AtomicCardTable

class MagicCardMapper(
    val ac: AtomicCardTable,
    val gson: Gson
) : MtgRecordMapper<MagicCard>() {

    private val cpp = Tables.CARD_PRINTING_PRICE
    private val cp = Tables.CARD_PRINTING

    private val eurPrice = DSL.select(DSL.min(cpp.PRICE_EUR_LAST_WEEK))
        .from(cpp)
        .join(cp).on(cp.UUID.eq(cpp.CARD_PRINTING))
        .where(cp.FULL_NAME.eq(ac.FULL_NAME))
        .asField<BigDecimal>("eurPrice")

    private val usdPrice = DSL.select(DSL.min(cpp.PRICE_USD_LAST_WEEK))
        .from(cpp)
        .join(cp).on(cp.UUID.eq(cpp.CARD_PRINTING))
        .where(cp.FULL_NAME.eq(ac.FULL_NAME))
        .asField<BigDecimal>("usdPrice")

    override val fields = listOf(ac.CARD_JSON, eurPrice, usdPrice)

    override fun mapData(r: Record): MagicCard {
        val faceJsons = gson.fromJson<JsonArray>(r[ac.CARD_JSON].data())
        val faces = faceJsons.map { it.obj.toMagicCardFace() }

        return MagicCard(
            faces,
            faceJsons[0]["legalities"].let { j ->
                MtgFormat.values().map {
                    val legalityStr = j.getOrNull(it.formatName)?.nullString
                    it to (legalityStr?.let { FormatLegality.fromName(it) } ?: FormatLegality.NOT_LEGAL)
                }.toMap()
            },
            CardLayout.fromDatabaseName(faceJsons.first()["layout"].string),
            faceJsons.first().obj.let {
                when {
                    it.has("edhrecRank") -> it["edhrecRank"].int
                    faces.first().superTypes.contains(SuperType.BASIC) -> 0
                    else -> 1000000
                }
            },
            eurPrice = r[eurPrice],
            usdPrice = r[usdPrice],
        )
    }
}

fun JsonObject.toMagicCardFace(): MagicCardFace {
    return MagicCardFace(
        name = this.getOrNull("faceName").nullString ?: this["name"].string,
        typeText = this["type"].string,
        types = this["types"].array.mapNotNull { CardType.byHumanReadableNameOptional(it.string) }.toSet(),
        text = this.getOrNull("text").nullString ?: "",
        superTypes = this["supertypes"].array.mapNotNull { SuperType.byHumanReadableNameOptional(it.string) }.toSet(),
        subTypes = this["subtypes"].array.mapNotNull { it.string }.toSet(),
        toughness = this.getOrNull("toughness").nullString,
        power = this.getOrNull("power").nullString,
        loyalty = this.getOrNull("loyalty").nullString,
        cost = this.getOrNull("manaCost").nullString,
        colorIdentity = this.getOrNull("colorIdentity")?.array?.map {
            CardColor.byLetter(it.string)
        }?.toSet() ?: emptySet(),
        colors = this.getOrNull("colors")?.array?.map {
            CardColor.byLetter(it.string)
        }?.toSet() ?: emptySet(),
        cmc = this["convertedManaCost"].float.roundToInt(),
        side = this.getOrNull("side").nullString ?: "default",
        leadershipSkills = this.getOrNull("leadershipSkills")?.obj?.let {
            setOfNotNull(
                if (it.getOrNull("brawl").nullBool == true) "brawl" else null,
                if (it.getOrNull("commander").nullBool == true) "commander" else null,
                if (it.getOrNull("oathbreaker").nullBool == true) "oathbreaker" else null,
            )
        } ?: emptySet(),
        isPlayable = when (CardLayout.fromDatabaseName(this["layout"].string)) {
            HOST, AUGMENT, SCHEME, PLANAR, VANGUARD, SAGA, NORMAL, MODAL, SPLIT, LEVELER, ADVENTURE -> true
            AFTERMATH, TRANSFORM, FLIP, MELD -> this.getOrNull("side").nullString == "a"
        },
        hasNoDeckRestriction = this.getOrNull("hasAlternativeDeckLimit").nullBool ?: false,
        edhrecRank = this.getOrNull("edhrecRank").nullInt
    )
}
