package com.wayfarer.boosterupgrade.cards

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.wayfarer.boosterupgrade.jooq.Tables.MTGJSON_UPDATE
import com.wayfarer.boosterupgrade.jooq.tables.records.AtomicCardRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.MtgjsonUpdateRecord
import com.wayfarer.boosterupgrade.util.getOrNull
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import com.wayfarer.boosterupgrade.util.jooq.executeDeferConstraints
import com.wayfarer.boosterupgrade.util.jooq.fetchSingleOrNull
import com.wayfarer.boosterupgrade.util.log
import org.jooq.DSLContext
import org.jooq.JSONB
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.net.URL
import java.util.zip.ZipInputStream
import javax.annotation.PostConstruct

@Repository
class MtgjsonUpdateRepository(ctx: DSLContext) : CrudJooqRepository<MtgjsonUpdateRecord>(ctx, MTGJSON_UPDATE) {

    val mu = MTGJSON_UPDATE.`as`("mu")

    fun findByVersion(version: String): MtgjsonUpdateRecord? {
        return ctx.selectFrom(mu)
            .where(mu.VERSION.eq(version))
            .limit(1)
            .fetchSingleOrNull()
    }

    fun findNewest(): MtgjsonUpdateRecord? {
        return ctx.selectFrom(mu)
            .orderBy(mu.UPDATED_ON.desc())
            .limit(1)
            .fetchSingleOrNull()
    }

    fun insertUpdate(releaseDate: String, version: String): Int {
        return ctx.insertInto(mu)
            .columns(mu.RELEASE_DATE, mu.VERSION)
            .values(releaseDate, version)
            .execute()
    }
}

data class MtgJsonVersionData(
    val date: String,
    val version: String
)

@Service
class CardService(
    val gson: Gson,
    val updateRepository: MtgjsonUpdateRepository,
    val ctx: DSLContext,
    val atomicCardRepository: AtomicCardRepository,
) {

    companion object {
        private const val ATOMIC_CARDS_JSON_URL = "https://mtgjson.com/api/v5/AtomicCards.json.zip"
        private const val META_JSON_URL = "https://mtgjson.com/api/v5/Meta.json"
    }

    private fun retrieveAtomicCards(): List<AtomicCardRecord> {
        log.info("Downloading atomic cards json")
        ZipInputStream(URL(ATOMIC_CARDS_JSON_URL).openStream()).use {
            it.nextEntry ?: error("Couldn't read atomic cards zip")
            val json = gson.fromJson<JsonObject>(it.bufferedReader())["data"].obj
            return json.entrySet().map { (_, cardJson) ->
                val faceJsons = cardJson.array
                val faces = faceJsons.sortedBy { it.getOrNull("side")?.nullString ?: "AAA" }

                fun JsonElement.faceName(): String {
                    return this.getOrNull("faceName")?.nullString ?: this["name"].string
                }

                AtomicCardRecord().apply {
                    this.fullName = faces.map { it.faceName() }.distinct().joinToString(" // ")
                    this.cardJson = JSONB.jsonb(JsonArray().apply {
                        faces.forEach { face ->
                            add(face)
                        }
                    }.toString())
                    this.names = faces.map { it.faceName() }.toTypedArray()
                    this.firstName = names.first()

                    if (!faces.first()["identifiers"].obj.has("scryfallOracleId")) {
                        error(cardJson.toString())
                    }

                    this.scryfallId = faces.first()["identifiers"]["scryfallOracleId"].string
                }
            }
        }
    }

    //TODO also on schedule
    @PostConstruct
    fun updateCardData() {
        ctx.transaction { conf ->
            val onlineVersion = try {
                gson.fromJson<MtgJsonVersionData>(
                    gson.fromJson<JsonObject>(
                        META_JSON_URL.httpGet().responseString().third.get()
                    )["meta"]
                )
            } catch (ex: FuelError) {
                log.error("Couldn't fetch meta json", ex)
                null
            }

            val currentVersion = updateRepository.findNewest()
            if (onlineVersion != null && (currentVersion == null || currentVersion.version != onlineVersion.version)) {
                ctx.executeDeferConstraints()

                val newCards = retrieveAtomicCards()
                log.info("Retrieved ${newCards.size} new cards")
                atomicCardRepository.deleteAll()
                val inserted = atomicCardRepository.insertAll(newCards)
                updateRepository.insertUpdate(onlineVersion.date, onlineVersion.version)
                log.info("Inserted $inserted cards")
            }
        }
    }

    fun correctCardNames(cardNames: List<String>): Map<String, String?> {
        val namesForCards = atomicCardRepository.fullNamesForCards(cardNames)

        val fixedNames = cardNames.associateWith { origName ->
            namesForCards.firstOrNull {
                it.first == origName || it.second == origName
            }?.first
        }

        return fixedNames
    }

}
