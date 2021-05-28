package com.wayfarer.boosterupgrade.cards

import com.wayfarer.boosterupgrade.util.withoutSections

val freeBasicLandNames = setOf("Mountain", "Plains", "Swamp", "Island", "Forest")

val String.firstNameOnly: String
    get() {
        return this.substringBefore(" // ")
    }

val MagicCard.firstName get() = faces.first().name
val MagicCard.fullName get() = faces.distinctBy { it.name }.joinToString(" // ") { it.name }
val MagicCard.playableFaces get() = faces.filter { it.isPlayable }
val MagicCard.isLand: Boolean get() = playableFaces.any { it.isLand }
val MagicCard.isFreeBasicLand: Boolean get() = firstName in freeBasicLandNames
val MagicCard.isCreature: Boolean get() = playableFaces.any { it.types.contains(CardType.CREATURE) }
val MagicCard.isBasicLand: Boolean
    get() = faces.first().let { it.types.contains(CardType.LAND) && it.superTypes.contains(SuperType.BASIC) }
val MagicCard.colorIdentity get() = faces.flatMap { it.colorIdentity }.toSet()
val MagicCard.colors get() = playableFaces.flatMap { it.colors }.toSet()

val MagicCardFace.isLand get() = types.contains(CardType.LAND)
val MagicCardFace.textWithoutReminderText: String
    get() = text.withoutSections("(", ")")

val MagicCardFace.normalizedText: String get() = textWithoutReminderText.replace(name, "~")

val MagicCard.isLegalCommander get() = this.faces.first().isLegalCommander

val MagicCardFace.isLegalCommander: Boolean
    get() = leadershipSkills.contains("commander")
