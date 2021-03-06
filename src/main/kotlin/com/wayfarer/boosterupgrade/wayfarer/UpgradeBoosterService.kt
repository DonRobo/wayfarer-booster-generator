package com.wayfarer.boosterupgrade.wayfarer

import com.wayfarer.boosterupgrade.cards.*
import com.wayfarer.boosterupgrade.edhrec.EdhRecRecommendation
import com.wayfarer.boosterupgrade.edhrec.EdhRecService
import org.springframework.stereotype.Service

@Service
class UpgradeBoosterService(
    private val edhRecService: EdhRecService,
) {

    fun generateBoosterForTheme(
        themeUrl: String,
        boosterPrice: Double,
        singlePrice: Double,
        useEuro: Boolean,
        usageWeight: Double,
        synergyWeight: Double,
        randomWeight: Double,
    ): List<MagicCard> {
        val recs = edhRecService.getRecommendationsFor(themeUrl)

        return generateBooster(
            recs = recs,
            boosterPriceLimit = boosterPrice,
            singlePriceLimit = singlePrice,
            useEuro = useEuro,
            usageWeight = usageWeight,
            synergyWeight = synergyWeight,
            randomWeight = randomWeight
        )
    }

    fun generateBoosterForPrecon(
        deckName: String,
        boosterPrice: Double,
        singlePrice: Double,
        useEuro: Boolean,
        usageWeight: Double,
        synergyWeight: Double,
        randomWeight: Double,
    ): List<MagicCard> {
        val recs = edhRecService.getRecommendationsForDeck(
            deckName = deckName,
            maxEurPrice = if (useEuro) boosterPrice else null,
            maxUsdPrice = if (!useEuro) boosterPrice else null
        )

        return generateBooster(
            recs = recs,
            boosterPriceLimit = boosterPrice,
            singlePriceLimit = singlePrice,
            useEuro = useEuro,
            usageWeight = usageWeight,
            synergyWeight = synergyWeight,
            randomWeight = randomWeight
        )
    }

    private fun generateBooster(
        recs: List<EdhRecRecommendation>,
        boosterPriceLimit: Double,
        singlePriceLimit: Double,
        useEuro: Boolean,
        usageWeight: Double,
        synergyWeight: Double,
        randomWeight: Double,
    ): List<MagicCard> {
        fun MagicCard.price() = (if (useEuro) eurPrice else usdPrice)?.toDouble() ?: 10.0

        val weightedRecs =
            recs.map { it to ((it.synergyScore / 100.0) * synergyWeight + (it.usageScore / 100.0) * usageWeight + Math.random() * randomWeight) }
                .filter {
                    it.first.card.price() <= singlePriceLimit
                }
                .sortedByDescending { it.second }

        val chosen = HashSet<MagicCard>()

        fun MagicCard.isCreature() = playableFaces.any { it.types.contains(CardType.CREATURE) }
        fun MagicCard.isLand() = playableFaces.first().isLand
        fun MagicCard.isNonCreature() = !isLand() && !isCreature()
        fun missingCards() = 15 - chosen.size
        fun missingLands() = 1 - chosen.count { it.playableFaces.first().isLand }
        fun missingCreatures() = 7 - chosen.count { it.playableFaces.first().types.contains(CardType.CREATURE) }
        fun missingNonCreatures() =
            7 - chosen.count { !it.playableFaces.first().isLand && !it.playableFaces.first().types.contains(CardType.CREATURE) }

        fun remainingBudget() = boosterPriceLimit - chosen.sumOf { it.price() }
        fun filteredWeightedRecs() =
            weightedRecs.filter { !it.first.card.isBasicLand && it.first.card !in chosen && it.first.card.price() <= remainingBudget() - missingCards() * 0.8 }
                .map { it.first }

        fun choose(block: (EdhRecRecommendation) -> Boolean) {
            val card = filteredWeightedRecs().first { block(it) }.card
            chosen += card
        }


        while (missingCards() > 0) {
            choose {
                //for safety
                if (missingLands() <= 0 && missingCreatures() <= 0 && missingNonCreatures() <= 0) return@choose true

                if (missingLands() <= 0 && it.card.isLand()) return@choose false
                if (missingCreatures() <= 0 && it.card.isCreature()) return@choose false
                if (missingNonCreatures() <= 0 && it.card.isNonCreature()) return@choose false

                return@choose true
            }
        }

        return chosen.sortedBy { it.edhRecRank }
    }

    fun generateBoosterForDeck(
        themeUrl: String,
        deck: List<String>,
        boosterPrice: Double,
        singlePrice: Double,
        useEuro: Boolean,
        usageWeight: Double,
        synergyWeight: Double,
        randomWeight: Double
    ): List<MagicCard> {
        val recs = edhRecService.getRecommendationsForTheme(
            theme = themeUrl,
            deckList = deck,
            maxEurPrice = if (useEuro) boosterPrice else null,
            maxUsdPrice = if (!useEuro) boosterPrice else null
        )

        return generateBooster(
            recs = recs,
            boosterPriceLimit = boosterPrice,
            singlePriceLimit = singlePrice,
            useEuro = useEuro,
            usageWeight = usageWeight,
            synergyWeight = synergyWeight,
            randomWeight = randomWeight
        )
    }

}
