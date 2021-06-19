package com.wayfarer.boosterupgrade.controller

import com.wayfarer.boosterupgrade.cards.CardService
import com.wayfarer.boosterupgrade.cards.MagicCard
import com.wayfarer.boosterupgrade.cards.firstName
import com.wayfarer.boosterupgrade.wayfarer.UpgradeBoosterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/booster")
class BoosterController(
    val boosterService: UpgradeBoosterService,
    val cardService: CardService,
    @Value("\${booster-price.eur}")
    private val boosterPriceEurDefault: Double,
    @Value("\${booster-price.usd}")
    private val boosterPriceUsdDefault: Double,
    @Value("\${single-price.usd}")
    private val singlePriceUsdDefault: Double,
    @Value("\${single-price.eur}")
    private val singlePriceEurDefault: Double,
    @Value("\${booster.usage-weight}")
    private val usageWeightDefault: Double,
    @Value("\${booster.synergy-weight}")
    private val synergyWeightDefault: Double,
    @Value("\${booster.random-weight}")
    private val randomWeightDefault: Double,
) {

    @GetMapping("generateBooster")
    fun generateBooster(
        @RequestParam deckName: String?,
        @RequestParam themeUrl: String?,
        @RequestBody(required = false) deckList: String?,
        @RequestParam(required = false, defaultValue = "true") useEuro: Boolean,
        @RequestParam(required = false) budgetLimit: Double?,
        @RequestParam(required = false) singleCardBudgetLimit: Double?,
        @RequestParam(required = false) usageWeight: Double?,
        @RequestParam(required = false) synergyWeight: Double?,
        @RequestParam(required = false) randomWeight: Double?,
    ): List<MagicCard> {
        return when {
            deckName != null && themeUrl != null -> error("Precons with custom themes not supported yet")
            deckName != null && deckList != null -> error("Precons with custom deckl ists not supported")
            deckName != null -> boosterService.generateBoosterForPrecon(
                deckName = deckName,
                boosterPrice = budgetLimit ?: if (useEuro) boosterPriceEurDefault else boosterPriceUsdDefault,
                singlePrice = singleCardBudgetLimit ?: if (useEuro) singlePriceEurDefault else singlePriceUsdDefault,
                useEuro = useEuro,
                usageWeight = usageWeight ?: usageWeightDefault,
                synergyWeight = synergyWeight ?: synergyWeightDefault,
                randomWeight = randomWeight ?: randomWeightDefault,
            )
            themeUrl != null -> boosterService.generateBoosterForDeck(
                themeUrl = themeUrl,
                deck = deckList?.split("\n") ?: emptyList(),
                boosterPrice = budgetLimit ?: if (useEuro) boosterPriceEurDefault else boosterPriceUsdDefault,
                singlePrice = singleCardBudgetLimit ?: if (useEuro) singlePriceEurDefault else singlePriceUsdDefault,
                useEuro = useEuro,
                usageWeight = usageWeight ?: usageWeightDefault,
                synergyWeight = synergyWeight ?: synergyWeightDefault,
                randomWeight = randomWeight ?: randomWeightDefault,
            )
            else -> error("Either deck name or commander and theme required")
        }
    }

    @GetMapping(
        "generateBoosterPlain",
        produces = [MediaType.TEXT_PLAIN_VALUE],
        consumes = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun generateBoosterPlain(
        @RequestParam deckName: String?,
        @RequestParam themeUrl: String?,
        @RequestBody(required = false) deckList: String?,
        @RequestParam(required = false, defaultValue = "true") useEuro: Boolean,
        @RequestParam(required = false) budgetLimit: Double?,
        @RequestParam(required = false) singleCardBudgetLimit: Double?,
        @RequestParam(required = false) usageWeight: Double?,
        @RequestParam(required = false) synergyWeight: Double?,
        @RequestParam(required = false) randomWeight: Double?,
    ): String {
        val gen = generateBooster(
            deckName = deckName,
            themeUrl = themeUrl,
            deckList = deckList,
            useEuro = useEuro,
            budgetLimit = budgetLimit,
            singleCardBudgetLimit = singleCardBudgetLimit,
            usageWeight = usageWeight,
            synergyWeight = synergyWeight,
            randomWeight = randomWeight
        )

        return gen.joinToString("\n") {
            "1 " + it.firstName
        }
    }

}
