package com.wayfarer.boosterupgrade.edhrec

import com.wayfarer.boosterupgrade.jooq.Tables
import com.wayfarer.boosterupgrade.jooq.Tables.EDHREC_RECOMMENDATION
import com.wayfarer.boosterupgrade.jooq.tables.records.EdhrecRecommendationRecord
import com.wayfarer.boosterupgrade.jooq.tables.records.EdhrecThemeRecord
import com.wayfarer.boosterupgrade.util.jooq.CrudJooqRepository
import com.wayfarer.boosterupgrade.util.jooq.MtgRecordMapper
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import com.wayfarer.boosterupgrade.jooq.tables.EdhrecRecommendation as EdhrecRecommendationTable

@Repository
class EdhRecThemeRepository(
    ctx: DSLContext
) : CrudJooqRepository<EdhrecThemeRecord>(ctx, Tables.EDHREC_THEME) {

}

class EdhRecRecommendationMapper(
    private val er: EdhrecRecommendationTable
) : MtgRecordMapper<EdhRecRecommendation>() {
    override val fields = listOf(er.CARD, er.USAGESCORE, er.SYNERGYSCORE)

    override fun mapData(r: Record): EdhRecRecommendation {
        return EdhRecRecommendation(r[er.CARD], r[er.SYNERGYSCORE], r[er.USAGESCORE])
    }
}

@Repository
class EdhRecRecommendationRepository(
    ctx: DSLContext
) : CrudJooqRepository<EdhrecRecommendationRecord>(ctx, EDHREC_RECOMMENDATION) {

    private val er = EDHREC_RECOMMENDATION.`as`("er")
    private val mapper = EdhRecRecommendationMapper(er)

    fun deleteByTheme(themeUrl: String): Int {
        return ctx.deleteFrom(er)
            .where(er.THEME.eq(themeUrl))
            .execute()
    }

    fun findByTheme(themeUrl: String): List<EdhRecRecommendation> {
        return ctx.select(mapper.fields)
            .from(er)
            .where(er.THEME.eq(themeUrl))
            .orderBy(er.USAGESCORE.desc(), er.SYNERGYSCORE.desc())
            .fetch(mapper)
    }

}
