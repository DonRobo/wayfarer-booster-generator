package com.wayfarer.boosterupgrade.precon

import com.google.gson.Gson
import com.wayfarer.boosterupgrade.cards.MagicCard
import com.wayfarer.boosterupgrade.cards.MagicCardMapper
import com.wayfarer.boosterupgrade.jooq.Tables.*
import com.wayfarer.boosterupgrade.jooq.tables.records.PreconCardRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.PreconRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PreconRepository(
    ctx: DSLContext,
    val gson: Gson,
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

    fun fetchPreconsWithCommander(): Map<String, MagicCard> {
        val ac = ATOMIC_CARD.`as`("ac")
        val aci = ATOMIC_CARD_ID.`as`("aci")
        val cardMapper = MagicCardMapper(ac, aci, gson)

        return ctx.select(cardMapper.fields + pc.PRECON)
            .from(pc)
            .join(ac).on(ac.FULL_NAME.eq(pc.CARD_NAME))
            .join(aci).on(ac.FULL_NAME.eq(aci.ATOMIC_CARD_NAME))
            .where(pc.COMMANDER)
            .fetch { r ->
                r[pc.PRECON] to cardMapper.map(r)
            }.toMap()
    }
}

@Repository
class PreconCardRepository(
    ctx: DSLContext
) : CrudJooqRepository<PreconCardRecord>(ctx, PRECON_CARD)
