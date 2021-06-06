package com.wayfarer.boosterupgrade

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BoosterUpgradeApplication

fun main(args: Array<String>) {
	runApplication<BoosterUpgradeApplication>(*args)
}
