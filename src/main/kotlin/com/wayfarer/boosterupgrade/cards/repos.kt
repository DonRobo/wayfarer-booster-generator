package com.wayfarer.boosterupgrade.cards

import com.google.gson.Gson
import com.wayfarer.boosterupgrade.jooq.Tables.*
import com.wayfarer.boosterupgrade.jooq.tables.records.AtomicCardRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.CardPrintingPriceRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.CardPrintingRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import org.jooq.DSLContext
import org.jooq.impl.DSL.notExists
import org.jooq.impl.DSL.select
import org.springframework.stereotype.Repository

@Repository
class AtomicCardRepository(ctx: DSLContext, val gson: Gson) : CrudJooqRepository<AtomicCardRecord>(ctx, ATOMIC_CARD) {

    val ac = ATOMIC_CARD.`as`("ac")

    fun fullNamesForCards(cards: List<String>): List<Pair<String, String>> {
        return ctx.select(ac.FULL_NAME, ac.FIRST_NAME)
            .from(ac)
            .where(ac.FULL_NAME.`in`(cards).or(ac.FIRST_NAME.`in`(cards)))
            .fetch { r ->
                r[ac.FULL_NAME] to r[ac.FIRST_NAME]
            }
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
