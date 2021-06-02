package com.wayfarer.boosterupgrade.controller

import com.wayfarer.boosterupgrade.cards.MagicCard
import com.wayfarer.boosterupgrade.cards.fullName
import com.wayfarer.boosterupgrade.edhrec.EdhRecService
import com.wayfarer.boosterupgrade.wayfarer.UpgradeBoosterService
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
) {

    @GetMapping("generateBooster")
    fun generateBooster(@RequestParam deckName: String): List<MagicCard> {
        return boosterService.generateBoosterForPrecon(deckName)
    }

    @GetMapping("generateBoosterPlain", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun generateBoosterPlain(@RequestParam deckName: String): String {
        return boosterService.generateBoosterForPrecon(deckName).joinToString("\n") { it.fullName }
    }

}
