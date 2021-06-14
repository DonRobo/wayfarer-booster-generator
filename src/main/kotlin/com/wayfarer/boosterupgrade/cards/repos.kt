package com.wayfarer.boosterupgrade.cards

import com.google.gson.Gson
import com.wayfarer.boosterupgrade.jooq.Tables.*
import com.wayfarer.boosterupgrade.jooq.tables.records.AtomicCardIdRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.AtomicCardRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.CardPrintingPriceRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.CardPrintingRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import com.wayfarer.boosterupgrade.util.jooq.jsonObject
import org.jooq.DSLContext
import org.jooq.impl.DSL.notExists
import org.jooq.impl.DSL.select
import org.springframework.stereotype.Repository

@Repository
class AtomicCardRepository(ctx: DSLContext, gson: Gson) : CrudJooqRepository<AtomicCardRecord>(ctx, ATOMIC_CARD) {

    private val ac = ATOMIC_CARD.`as`("ac")
    private val aci = ATOMIC_CARD_ID.`as`("aci")
    private val mapper = MagicCardMapper(ac, aci, gson)

    fun fullNamesForCards(cards: List<String>): List<Pair<String, String>> {
        return ctx.select(ac.FULL_NAME, ac.FIRST_NAME)
            .from(ac)
            .where(ac.FULL_NAME.`in`(cards).or(ac.FIRST_NAME.`in`(cards)))
            .fetch { r ->
                r[ac.FULL_NAME] to r[ac.FIRST_NAME]
            }
    }

    fun findByAtomicCardIds(cardIds: List<Int>): List<MagicCard> {
        return ctx.select(mapper.fields)
            .from(ac)
            .join(aci).on(ac.FULL_NAME.eq(aci.ATOMIC_CARD_NAME))
            .where(aci.ID.`in`(cardIds))
            .fetch(mapper)
    }

}

@Repository
class CardPrintingRepository(ctx: DSLContext) : CrudJooqRepository<CardPrintingRecord>(ctx, CARD_PRINTING)

@Repository
class CardPriceRepository(ctx: DSLContext) : CrudJooqRepository<CardPrintingPriceRecord>(ctx, CARD_PRINTING_PRICE) {

    private val cpp = CARD_PRINTING_PRICE.`as`("cpp")

    fun deleteCardlessPrices(): Int {
        val cp = CARD_PRINTING.`as`("cp")

        return ctx.deleteFrom(cpp)
            .where(
                notExists(
                    select(cp.UUID)
                        .from(cp)
                        .where(cp.UUID.eq(cpp.CARD_PRINTING))
                )
            )
            .execute()
    }
}

@Repository
class CardIdRepository(ctx: DSLContext) : CrudJooqRepository<AtomicCardIdRecord>(ctx, ATOMIC_CARD_ID) {

    private val aci = ATOMIC_CARD_ID.`as`("aci")
    private val ac = ATOMIC_CARD.`as`("ac")

    fun updateCardIds(): Int {
        val inserted = ctx.insertInto(ATOMIC_CARD_ID, ATOMIC_CARD_ID.ATOMIC_CARD_NAME)
            .select(
                select(ac.FULL_NAME)
                    .from(ac)
                    .where(
                        notExists(
                            select(aci.ID)
                                .from(aci)
                                .where(ac.FULL_NAME.eq(aci.ATOMIC_CARD_NAME))
                        )
                    )
                    .orderBy(ac.CARD_JSON.jsonObject(0).jsonObject("edhrecRank").asc())
            )
            .execute()
        val removed = ctx.deleteFrom(aci)
            .where(notExists(select(ac.FULL_NAME).from(ac).where(ac.FULL_NAME.eq(aci.ATOMIC_CARD_NAME))))
            .execute()

        return inserted + removed
    }

}
