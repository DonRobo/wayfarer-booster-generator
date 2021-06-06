package com.wayfarer.boosterupgrade.controller

import com.wayfarer.boosterupgrade.cards.MagicCard
import com.wayfarer.boosterupgrade.cards.fullName
import com.wayfarer.boosterupgrade.edhrec.EdhRecService
import com.wayfarer.boosterupgrade.util.asEuros
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
    val edhRecService: EdhRecService,
    val boosterService: UpgradeBoosterService,
    @Value("\${booster-price.eur}")
    private val boosterPriceEur: Double,
    @Value("\${booster-price.usd}")
    private val boosterPriceUsd: Double,
) {

    @GetMapping("generateBooster")
    fun generateBooster(
        @RequestParam deckName: String,
        @RequestParam(required = false, defaultValue = "true") useEuro: Boolean = true,
    ): List<MagicCard> {
        return boosterService.generateBoosterForPrecon(
            deckName,
            if (useEuro) boosterPriceEur else boosterPriceUsd,
            useEuro
        )
    }

    @GetMapping("generateBoosterPlain", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun generateBoosterPlain(
        @RequestParam deckName: String,
        @RequestParam(required = false, defaultValue = "true") useEuro: Boolean
    ): String {
        val gen = boosterService.generateBoosterForPrecon(
            deckName,
            if (useEuro) boosterPriceEur else boosterPriceUsd,
            useEuro
        )

        return gen.joinToString("\n") {
            it.fullName + " " + it.eurPrice.asEuros
        } + "\n${gen.sumOf { it.eurPrice }}"
    }

}
