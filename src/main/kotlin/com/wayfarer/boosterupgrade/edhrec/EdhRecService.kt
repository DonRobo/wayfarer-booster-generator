package com.wayfarer.boosterupgrade.edhrec

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.wayfarer.boosterupgrade.cards.CardService
import com.wayfarer.boosterupgrade.jooq.tables.records.EdhrecRecommendationRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.EdhrecThemeRecord
import com.wayfarer.boosterupgrade.util.log
import com.wayfarer.boosterupgrade.util.olderThan
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class EdhRecService(
    val gson: Gson,
    val cardService: CardService,
    val edhRecThemeRepository: EdhRecThemeRepository,
    val edhRecRecommendationRepository: EdhRecRecommendationRepository,
    @Value("\${update-frequency.edhrec.themes}")
    private val edhRecThemeUpdateTime: Duration,
    val ctx: DSLContext,
) {

    private fun updateRecommendationsFor(themeUrl: String) {
        ctx.transaction { conf ->
            require(themeUrl.startsWith("commanders/")) {
                "Invalid commander theme $themeUrl"
            }
            require(themeUrl.endsWith(".json")) {
                "Invalid commander theme file $themeUrl"
            }

            val fetched = edhRecThemeRepository.findByIdOptional(themeUrl)
            if (fetched != null && !fetched.fetchedOn.olderThan(edhRecThemeUpdateTime)) {
                return@transaction
            }
            log.info("Fetching edhrec data for $themeUrl")

            val (_, _, content) = "https://edhrec-json.s3.amazonaws.com/en/$themeUrl".httpGet().responseString()

            val json = gson.fromJson<JsonObject>(content.get())

            val parserRegex = Regex("(\\d+)% of \\d+ decks\n(([+\\-])\\d+)% synergy")

            data class EdhRecommendation(
                val cardName: String,
                val synergy: Int,
                val usage: Int
            )

            val recs = json["container"]["json_dict"]["cardlists"].array.flatMap { cardList ->
                cardList["cardviews"].array.mapNotNull { cardView ->
                    val label = cardView["label"].string
                    val match = parserRegex.matchEntire(label) ?: return@mapNotNull null
                    EdhRecommendation(
                        cardView["names"].array.joinToString(" // ") { it.string },
                        match.groupValues[2].toInt(),
                        match.groupValues[1].toInt(),
                    )
                }
            }
            val cardObj = json["container"]["json_dict"]["card"]

            val commander: String
            val partner: String?

            if (cardObj.obj.has("cards")) {
                val cards = cardObj["cards"].array
                require(cards.size() == 2)

                commander = cards[0]["name"].string
                partner = cards[1]["name"].string
            } else {
                commander = cardObj["name"].string
                partner = null
            }

            log.info("Detected commander $commander" + (partner?.let { " with partner $it" } ?: ""))

            if (partner != null) {
                log.warn("No partner support yet")
            }

            val cardNameCorrections =
                cardService.correctCardNames(recs.map { it.cardName } + commander) //no partner support for now
            val unknownNames = cardNameCorrections.filter { it.value == null }
            require(unknownNames.isEmpty()) {
                "Unknown names: ${unknownNames.keys}"
            }

            val themeRegex = Regex("commanders/([^/.]+)(/[^/.]+)?\\.json")
            val themeFromUrl = themeRegex.matchEntire(themeUrl)?.groupValues?.get(2)?.ifBlank { null } ?: "Generic"

            val records = recs.map {
                EdhrecRecommendationRecord(
                    themeUrl,
                    cardNameCorrections[it.cardName]!!,
                    it.synergy,
                    it.usage
                )
            }
            log.info("Found ${records.size} recommendations")
            edhRecThemeRepository.upsert(
                EdhrecThemeRecord(
                    themeUrl,
                    cardNameCorrections[commander]!!,
                    null,
                    themeFromUrl,
                    LocalDateTime.now()
                )
            )
            edhRecRecommendationRepository.deleteByTheme(themeUrl)
            edhRecRecommendationRepository.insertAll(records)
        }
    }

    fun getRecommendationsFor(themeUrl: String): List<EdhRecRecommendation> {
        updateRecommendationsFor(themeUrl)
        return edhRecRecommendationRepository.findByTheme(themeUrl)
    }

}
