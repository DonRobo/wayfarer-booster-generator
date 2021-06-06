package com.wayfarer.boosterupgrade.edhrec

import com.google.gson.Gson
import com.wayfarer.boosterupgrade.cards.EUR_PRICE
import com.wayfarer.boosterupgrade.cards.MagicCardMapper
import com.wayfarer.boosterupgrade.cards.USD_PRICE
import com.wayfarer.boosterupgrade.jooq.Tables.*
import com.wayfarer.boosterupgrade.jooq.tables.records.EdhrecRecommendationRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.EdhrecThemeRecord
import com.wayfarer.boosterupgrade.util.chainIfNotNull
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import com.wayfarer.boosterupgrade.util.jooq.MtgRecordMapper
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import com.wayfarer.boosterupgrade.jooq.tables.EdhrecRecommendation as EdhrecRecommendationTable

@Repository
class EdhRecThemeRepository(
    ctx: DSLContext
) : CrudJooqRepository<EdhrecThemeRecord>(ctx, EDHREC_THEME) {

    private val et = EDHREC_THEME.`as`("et")
    private val pcc = PRECON_CARD.`as`("pcc")

    fun findByPrecon(deckName: String): EdhrecThemeRecord? {
        return ctx.select(et.asterisk())
            .from(et)
            .join(pcc).on(et.COMMANDER.eq(pcc.CARD_NAME).and(pcc.COMMANDER))
            .where(pcc.PRECON.eq(deckName))
            .fetchOptionalInto(et)
            .orElse(null)
    }

}

class EdhRecRecommendationMapper(
    private val er: EdhrecRecommendationTable,
    private val cardMapper: MagicCardMapper,
) : MtgRecordMapper<EdhRecRecommendation>() {
    override val fields = listOf(er.CARD, er.USAGESCORE, er.SYNERGYSCORE) + cardMapper.fields

    override fun mapData(r: Record): EdhRecRecommendation {
        return EdhRecRecommendation(cardMapper.map(r), r[er.SYNERGYSCORE], r[er.USAGESCORE])
    }
}

@Repository
class EdhRecRecommendationRepository(
    ctx: DSLContext,
    private val gson: Gson
) : CrudJooqRepository<EdhrecRecommendationRecord>(ctx, EDHREC_RECOMMENDATION) {

    private val er = EDHREC_RECOMMENDATION.`as`("er")
    private val ac = ATOMIC_CARD.`as`("ac")
    private val pc = PRECON_CARD.`as`("pc")
    private val et = EDHREC_THEME.`as`("et")
    private val mapper = EdhRecRecommendationMapper(er, MagicCardMapper(ac, gson))

    fun deleteByTheme(themeUrl: String): Int {
        return ctx.deleteFrom(er)
            .where(er.THEME.eq(themeUrl))
            .execute()
    }

    fun findByTheme(themeUrl: String): List<EdhRecRecommendation> {
        return ctx.select(mapper.fields)
            .from(er)
            .join(ac).on(ac.FULL_NAME.eq(er.CARD))
            .where(er.THEME.eq(themeUrl))
            .orderBy(er.USAGESCORE.desc(), er.SYNERGYSCORE.desc())
            .fetch(mapper)
    }

    fun findByDeck(deckName: String, maxEurPrice: Double?, maxUsdPrice: Double?): List<EdhRecRecommendation> {
        val pc2 = PRECON_CARD.`as`("pc2")

        return ctx.select(mapper.fields)
            .from(er)
            .join(ac).on(ac.FULL_NAME.eq(er.CARD))
            .join(et).on(et.URL.eq(er.THEME))
            .join(pc).on(et.COMMANDER.eq(pc.CARD_NAME).and(pc.COMMANDER))
            .where(pc.PRECON.eq(deckName))
            .and(
                notExists(
                    select(asterisk())
                        .from(pc2)
                        .where(pc2.PRECON.eq(pc.PRECON))
                        .and(pc2.CARD_NAME.eq(er.CARD))
                )
            )
            .chainIfNotNull(maxEurPrice) {
                and(ac.EUR_PRICE.asField<BigDecimal>().lessOrEqual(it.toBigDecimal()))
            }
            .chainIfNotNull(maxUsdPrice) {
                and(ac.USD_PRICE.asField<BigDecimal>().lessOrEqual(it.toBigDecimal()))
            }
            .orderBy(er.USAGESCORE.desc(), er.SYNERGYSCORE.desc())
            .fetch(mapper)
    }

}
