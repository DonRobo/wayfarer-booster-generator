package com.wayfarer.boosterupgrade.cards

import com.google.gson.Gson
import com.wayfarer.boosterupgrade.jooq.Tables.ATOMIC_CARD
import com.wayfarer.boosterupgrade.jooq.tables.records.AtomicCardRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AtomicCardRepository(ctx: DSLContext, val gson: Gson) : CrudJooqRepository<AtomicCardRecord>(ctx, ATOMIC_CARD)
