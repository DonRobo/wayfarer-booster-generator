package com.wayfarer.boosterupgrade.cards

import com.google.gson.Gson
import com.wayfarer.boosterupgrade.jooq.Tables.ATOMIC_CARD
import com.wayfarer.boosterupgrade.jooq.tables.records.AtomicCardRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import org.jooq.DSLContext
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
