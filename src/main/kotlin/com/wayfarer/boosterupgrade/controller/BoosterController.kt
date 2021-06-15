package com.wayfarer.boosterupgrade.controller

import com.wayfarer.boosterupgrade.cards.CardService
import com.wayfarer.boosterupgrade.cards.MagicCard
import com.wayfarer.boosterupgrade.cards.firstName
import com.wayfarer.boosterupgrade.wayfarer.UpgradeBoosterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
        @RequestParam deckName: String,
        @RequestParam(required = false, defaultValue = "true") useEuro: Boolean,
        @RequestParam(required = false) budgetLimit: Double?,
        @RequestParam(required = false) singleCardBudgetLimit: Double?,
        @RequestParam(required = false) usageWeight: Double?,
        @RequestParam(required = false) synergyWeight: Double?,
        @RequestParam(required = false) randomWeight: Double?,
    ): List<MagicCard> {
        return boosterService.generateBoosterForPrecon(
            deckName,
            budgetLimit ?: if (useEuro) boosterPriceEurDefault else boosterPriceUsdDefault,
            singleCardBudgetLimit ?: if (useEuro) singlePriceEurDefault else singlePriceUsdDefault,
            useEuro,
            usageWeight ?: usageWeightDefault,
            synergyWeight ?: synergyWeightDefault,
            randomWeight ?: randomWeightDefault,
        )
    }

    @GetMapping("generateBoosterPlain", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun generateBoosterPlain(
        @RequestParam deckName: String,
        @RequestParam(required = false, defaultValue = "true") useEuro: Boolean,
        @RequestParam(required = false) budgetLimit: Double?,
        @RequestParam(required = false) singleCardBudgetLimit: Double?,
        @RequestParam(required = false) usageWeight: Double?,
        @RequestParam(required = false) synergyWeight: Double?,
        @RequestParam(required = false) randomWeight: Double?,
    ): String {
        val gen = boosterService.generateBoosterForPrecon(
            deckName,
            budgetLimit ?: if (useEuro) boosterPriceEurDefault else boosterPriceUsdDefault,
            singleCardBudgetLimit ?: if (useEuro) singlePriceEurDefault else singlePriceUsdDefault,
            useEuro,
            usageWeight ?: usageWeightDefault,
            synergyWeight ?: synergyWeightDefault,
            randomWeight ?: randomWeightDefault,
        )


        return gen.joinToString("\n") {
            "1 " + it.firstName
        }
    }

}
