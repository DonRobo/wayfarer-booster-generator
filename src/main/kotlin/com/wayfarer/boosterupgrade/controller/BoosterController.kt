package com.wayfarer.boosterupgrade.controller

import com.wayfarer.boosterupgrade.edhrec.EdhRecRecommendation
import com.wayfarer.boosterupgrade.edhrec.EdhRecService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/booster")
class BoosterController(
    val edhRecService: EdhRecService,
) {

    @GetMapping("test")
    fun testEndpoint(@RequestParam themeUrl: String): List<EdhRecRecommendation> {
        return edhRecService.getRecommendationsFor(themeUrl)
    }

}
