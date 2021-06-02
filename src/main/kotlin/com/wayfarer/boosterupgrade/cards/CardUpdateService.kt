package com.wayfarer.boosterupgrade.cards

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.wayfarer.boosterupgrade.jooq.tables.records.AtomicCardRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.PreconCardRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.PreconRecord
import com.wayfarer.boosterupgrade.precon.PreconCardRepository
import com.wayfarer.boosterupgrade.precon.PreconRepository
import com.wayfarer.boosterupgrade.util.getOrNull
import com.wayfarer.boosterupgrade.util.jooq.executeDeferConstraints
import com.wayfarer.boosterupgrade.util.log
import org.jooq.DSLContext
import org.jooq.JSONB
import org.springframework.stereotype.Component
import java.net.URL
import java.util.zip.ZipInputStream
import javax.annotation.PostConstruct

@Component
class CardUpdater(
    val gson: Gson,
    val updateRepository: MtgjsonUpdateRepository,
    val ctx: DSLContext,
    val atomicCardRepository: AtomicCardRepository,
    val preconRepository: PreconRepository,
    val preconCardRepository: PreconCardRepository,
) {
    companion object {
        private const val ATOMIC_CARDS_JSON_URL = "https://mtgjson.com/api/v5/AtomicCards.json.zip"
        private const val PRECONS_JSON_URL = "https://mtgjson.com/api/v5/AllDeckFiles.zip"
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

                updatePrecons()
            }
        }
    }

    private fun updatePrecons() {
        preconCardRepository.deleteAll()
        preconRepository.deleteAll()

        val alreadyInserted = mutableSetOf<String>()

        log.info("Downloading precon jsons")
        ZipInputStream(URL(PRECONS_JSON_URL).openStream()).use {
            var nextEntry = it.nextEntry
            var insertedPrecons = 0
            while (nextEntry != null) {
                val json = gson.fromJson<JsonObject>(it.reader())
                val data = json["data"].obj

                val preconName = data["name"].string
                val type = data["type"].string

                if (type == "Commander Deck" && alreadyInserted.add(preconName)) {
                    log.debug("Parsing deck \"$preconName\" (${nextEntry.name})")
                    val pr = PreconRecord().apply {
                        this.name = preconName
                    }
                    val insertedPrecon = preconRepository.insert(pr)
                    val commanders = data["commander"].array.map {
                        it.obj.let { com ->
                            com["name"].string to com["count"].int
                        }
                    }
                    val cards = data["mainBoard"].array.map {
                        it.obj.let { card ->
                            card["name"].string to card["count"].int
                        }
                    }
                    val cardRecords = commanders.map { (card, count) ->
                        PreconCardRecord().apply {
                            this.precon = preconName
                            this.commander = true
                            this.cardName = card
                            this.count = count
                        }
                    } + cards.map { (card, count) ->
                        PreconCardRecord().apply {
                            this.precon = preconName
                            this.commander = false
                            this.cardName = card
                            this.count = count
                        }
                    }
                    preconCardRepository.insertAll(cardRecords)
                    insertedPrecons++
                }
                nextEntry = it.nextEntry
            }
            log.info("Loaded $insertedPrecons commander precons")
        }
    }

}
