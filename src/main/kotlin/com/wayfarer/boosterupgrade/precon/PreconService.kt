package com.wayfarer.boosterupgrade.precon

import com.wayfarer.boosterupgrade.cards.MagicCard
import org.springframework.stereotype.Service

@Service
class PreconService(
    private val preconRepository: PreconRepository,
) {
    fun preconsWithCommander(): Map<String, MagicCard> {
        return preconRepository.fetchPreconsWithCommander()
    }

    fun precons(): List<String> {
        return preconRepository.selectAll().map { it.name }
    }
}
