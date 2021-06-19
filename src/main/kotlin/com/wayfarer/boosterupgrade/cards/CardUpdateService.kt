package com.wayfarer.boosterupgrade.cards

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.wayfarer.boosterupgrade.jooq.tables.records.*
import com.wayfarer.boosterupgrade.precon.PreconCardRepository
import com.wayfarer.boosterupgrade.precon.PreconRepository
import com.wayfarer.boosterupgrade.util.*
import com.wayfarer.boosterupgrade.util.jooq.executeDeferConstraints
import org.jooq.DSLContext
import org.jooq.JSONB
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipInputStream
import javax.annotation.PostConstruct
import kotlin.concurrent.withLock

@Component
class CardUpdater(
    val gson: Gson,
    val updateRepository: MtgjsonUpdateRepository,
    val ctx: DSLContext,
    val atomicCardRepository: AtomicCardRepository,
    val preconRepository: PreconRepository,
    val preconCardRepository: PreconCardRepository,
    val cardPrintingRepository: CardPrintingRepository,
    val cardPriceRepository: CardPriceRepository,
    val cardIdRepository: CardIdRepository,
) {
    companion object {
        private const val ATOMIC_CARDS_JSON_URL = "https://mtgjson.com/api/v5/AtomicCards.json.zip"
        private const val CARD_PRINTINGS_JSON_URL = "https://mtgjson.com/api/v5/AllPrintings.json.zip"
        private const val CARD_PRICES_JSON_URL = "https://mtgjson.com/api/v5/AllPrices.json.zip"
        private const val PRECONS_JSON_URL = "https://mtgjson.com/api/v5/AllDeckFiles.zip"
        private const val META_JSON_URL = "https://mtgjson.com/api/v5/Meta.json"
    }

    private val lock = ReentrantLock()

    @PostConstruct
    @Scheduled(cron = "10 10 2 * * *")
    fun updateCardData() {
        lock.withLock {
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
                    log.info("Updating from ${currentVersion?.version ?: "<no cards installed>"} to ${onlineVersion.version}")

                    ctx.executeDeferConstraints()

                    updateAtomicCards()
                    updatePrecons()
                    updateCardPrintings()
                    updateCardPrices()

                    val updatedIds = cardIdRepository.updateCardIds()
                    log.info("Updated $updatedIds card ids")

                    updateRepository.insertUpdate(onlineVersion.date, onlineVersion.version)
                }
            }
        }
    }

    private fun updateAtomicCards() {
        log.info("Downloading atomic cards json")

        atomicCardRepository.deleteAll()

        val records = ArrayList<AtomicCardRecord>()
        fun flushRecords() {
            atomicCardRepository.insertAll(records)
            log.info("Inserted ${records.size} atomic cards")
            records.clear()
        }

        ZipInputStream(URL(ATOMIC_CARDS_JSON_URL).openStream()).apply { requireNotNull(nextEntry) }.reader()
            .use { reader ->
                val jsonReader = gson.newJsonReader(reader)
                jsonReader.beginObject()
                jsonReader.untilProperty("data")
                jsonReader.beginObject()

                while (jsonReader.hasNext()) {
                    val cardName = jsonReader.nextName()
                    val cardJson = gson.fromJson<JsonArray>(jsonReader)

                    val faceJsons = cardJson.array
                    val faces = faceJsons.sortedBy { it.getOrNull("side")?.nullString ?: "AAA" }

                    fun JsonElement.faceName(): String {
                        return getOrNull("faceName")?.nullString ?: this["name"].string
                    }

                    records += AtomicCardRecord().apply {
                        fullName = cardName
                        this.cardJson = JSONB.jsonb(JsonArray().apply {
                            faces.forEach { face ->
                                add(face)
                            }
                        }.toString())
                        names = faces.map { it.faceName() }.toTypedArray()
                        firstName = names.first()

                        if (!faces.first()["identifiers"].obj.has("scryfallOracleId")) {
                            error(cardJson.toString())
                        }

                        scryfallId = faces.first()["identifiers"]["scryfallOracleId"].string
                    }

                    if (records.size >= 1000) {
                        flushRecords()
                    }
                }
            }

        flushRecords()
    }

    private fun updateCardPrintings() {
        log.info("Downloading all printings JSON")

        cardPrintingRepository.deleteAll()

        val alreadyInserted = mutableSetOf<UUID>()
        val records = ArrayList<CardPrintingRecord>()
        fun flushRecords() {
            cardPrintingRepository.insertAll(records.filter { it.uuid !in alreadyInserted }.distinctBy { it.uuid }
                .also {
                    alreadyInserted.addAll(it.map { it.uuid })
                })
            log.info("Inserted ${records.size} card printings")
            records.clear()
        }

        ZipInputStream(URL(CARD_PRINTINGS_JSON_URL).openStream()).apply { requireNotNull(nextEntry) }.reader()
            .use { reader ->
                val jsonReader = gson.newJsonReader(reader)
                jsonReader.beginObject()
                jsonReader.untilProperty("data")
                jsonReader.beginObject()

                while (jsonReader.hasNext()) {
                    val setCode = jsonReader.nextName()
                    val setJson = gson.fromJson<JsonObject>(jsonReader)
                    val name = setJson["name"].string
                    if (setJson["type"].string != "memorabilia") {
                        setJson["cards"].array.forEach { cardJson ->
                            records += CardPrintingRecord().apply {
                                this.uuid = UUID.fromString(cardJson["uuid"].string)
                                this.faceName = cardJson.getOrNull("faceName")?.nullString ?: cardJson["name"].string
                                this.collectorNumber = cardJson["number"].string
                                this.setCode = setCode
                                this.fullName = cardJson["name"].string
                            }
                        }
                    } else {
                        log.debug("Skipping memorabilia set $name")
                    }

                    if (records.size >= 1000) {
                        flushRecords()
                    }
                }
                jsonReader.endObject()
                jsonReader.close()
                flushRecords()
            }
    }

    private fun updateCardPrices() {
        log.info("Downloading all prices JSON")

        cardPriceRepository.deleteAll()

        val records = ArrayList<CardPrintingPriceRecord>()
        fun flushRecords() {
            cardPriceRepository.insertAll(records)
            log.info("Inserted ${records.size} card prices")
            records.clear()
        }

        ZipInputStream(URL(CARD_PRICES_JSON_URL).openStream()).apply { requireNotNull(nextEntry) }.reader()
            .use { reader ->
                val jsonReader = gson.newJsonReader(reader)
                jsonReader.beginObject()
                jsonReader.untilProperty("data")
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    val printingUUID = UUID.fromString(jsonReader.nextName())
                    val cardData = gson.fromJson<JsonObject>(jsonReader).getOrNull("paper").nullObj
                    val mkm = cardData?.getOrNull("cardmarket")?.getOrNull("retail")?.nullObj
                    val tcg = cardData?.getOrNull("tcgplayer")?.getOrNull("retail")?.nullObj

                    val eurPrices = ArrayList<Pair<LocalDate, Double>>()
                    val eurFoilPrices = ArrayList<Pair<LocalDate, Double>>()
                    val usdPrices = ArrayList<Pair<LocalDate, Double>>()
                    val usdFoilPrices = ArrayList<Pair<LocalDate, Double>>()

                    fun ArrayList<Pair<LocalDate, Double>>.addEntries(provider: JsonObject?, type: String) {
                        provider?.getOrNull(type)?.nullObj?.toMap()?.entries?.forEach {
                            val date = parseIsoDate(it.key)
                            val price = it.value.double
                            add(date to price)
                        }
                    }

                    eurPrices.addEntries(mkm, "normal")
                    eurFoilPrices.addEntries(mkm, "foil")
                    usdPrices.addEntries(tcg, "normal")
                    usdFoilPrices.addEntries(tcg, "foil")

                    val eurPrice = listOfNotNull(eurPrices.maxByOrNull { it.first },
                        eurFoilPrices.maxByOrNull { it.first }).minByOrNull { it.second }?.second
                    val usdPrice = listOfNotNull(usdPrices.maxByOrNull { it.first },
                        usdFoilPrices.maxByOrNull { it.first }).minByOrNull { it.second }?.second

                    fun lastWeek(vararg prices: List<Pair<LocalDate, Double>>): Double? {
                        val dailyPrices = prices.flatMap { it }.groupBy { it.first }
                            .mapValues { it.value.minByOrNull { it.second }?.second }

                        val lastWeeksPrices =
                            dailyPrices.filter { it.value != null && !it.key.olderThanDays(7) }
                                .mapNotNull {
                                    it.value ?: return@mapNotNull null
                                }
                        return lastWeeksPrices.median()
                    }

                    records += CardPrintingPriceRecord().apply {
                        this.cardPrinting = printingUUID
                        this.priceEur = eurPrice?.toBigDecimal()
                        this.priceEurLastWeek = lastWeek(eurPrices, eurFoilPrices)?.toBigDecimal()
                        this.priceUsd = usdPrice?.toBigDecimal()
                        this.priceUsdLastWeek = lastWeek(usdPrices, usdFoilPrices)?.toBigDecimal()
                        this.updatedOn =
                            ((eurPrices + eurFoilPrices + usdPrices + usdFoilPrices).maxByOrNull { it.first }?.first
                                ?: LocalDate.now()).atTime(LocalTime.now())
                    }

                    if (records.size >= 1000)
                        flushRecords()
                }
            }
        flushRecords()

        cardPriceRepository.deleteCardlessPrices()
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
                    preconRepository.insert(pr)
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
