package com.wayfarer.boosterupgrade.controller

import com.wayfarer.boosterupgrade.cards.MagicCard
import com.wayfarer.boosterupgrade.precon.PreconService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/precons")
@CrossOrigin
class PreconController(
    val preconService: PreconService,
) {

    @GetMapping("withCommander")
    fun listPreconsWithCommander(): Map<String, MagicCard> {
        return preconService.preconsWithCommander()
    }

    @GetMapping
    fun listPrecons(): List<String> {
        return preconService.precons()
    }

}
