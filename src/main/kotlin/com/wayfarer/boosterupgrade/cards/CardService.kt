package com.wayfarer.boosterupgrade.cards

import com.google.gson.Gson
import com.wayfarer.boosterupgrade.jooq.Tables.MTGJSON_UPDATE
import com.wayfarer.boosterupgrade.jooq.tables.records.MtgjsonUpdateRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import com.wayfarer.boosterupgrade.util.jooq.fetchSingleOrNull
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

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
    val atomicCardRepository: AtomicCardRepository,
) {

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
