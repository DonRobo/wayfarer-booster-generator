package com.wayfarer.boosterupgrade.wayfarer

import com.wayfarer.boosterupgrade.cards.MagicCard
import com.wayfarer.boosterupgrade.cards.isBasicLand
import com.wayfarer.boosterupgrade.cards.isCreature
import com.wayfarer.boosterupgrade.cards.isLand
import com.wayfarer.boosterupgrade.edhrec.EdhRecRecommendation
import com.wayfarer.boosterupgrade.edhrec.EdhRecService
import org.springframework.stereotype.Service

@Service
class UpgradeBoosterService(
    private val edhRecService: EdhRecService,
) {

    fun generateBoosterForTheme(themeUrl: String): List<MagicCard> {
        val recs = edhRecService.getRecommendationsFor(themeUrl)

        return generateBooster(recs)
    }

    fun generateBoosterForPrecon(deckName: String): List<MagicCard> {
        val recs = edhRecService.getRecommendationsForDeck(deckName)

        return generateBooster(recs)
    }

    private fun generateBooster(recs: List<EdhRecRecommendation>): List<MagicCard> {
        val recommendedLand = recs.filter { it.card.isLand && !it.card.isBasicLand }.random()
        val creatures = recs.filter { it.card.isCreature }.shuffled().take(7)
        val nonCreatures = recs.filter { !it.card.isCreature }.shuffled().take(7)

        return (creatures + nonCreatures + recommendedLand).map { it.card }
    }

}
