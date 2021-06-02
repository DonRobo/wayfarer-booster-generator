package com.wayfarer.boosterupgrade.precon

import com.wayfarer.boosterupgrade.jooq.Tables.PRECON
import com.wayfarer.boosterupgrade.jooq.Tables.PRECON_CARD
import com.wayfarer.boosterupgrade.jooq.tables.records.PreconCardRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.PreconRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PreconRepository(
    ctx: DSLContext
) : CrudJooqRepository<PreconRecord>(ctx, PRECON) {
    private val pc = PRECON_CARD.`as`("pc")

    fun findCommanderForDeck(deckName: String): String {
        return ctx.select(pc.CARD_NAME)
            .from(pc)
            .where(pc.COMMANDER)
            .and(pc.PRECON.eq(deckName))
            .fetchSingle()
            .value1()
    }
}

@Repository
class PreconCardRepository(
    ctx: DSLContext
) : CrudJooqRepository<PreconCardRecord>(ctx, PRECON_CARD)
