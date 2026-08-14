/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.text.MessageFormat
import java.util.Locale
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import net.particify.arsnova.core4.room.RoomUserAlias
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

/**
 * Generates localized aliases which are composed of an adjective and an animal name. The generated
 * name is fully determined by the alias' seed so it stays stable while its localization can change.
 */
@Component
class RoomUserAliasGenerator(
    aliasWordsProperties: AliasWordsProperties,
    @Qualifier("aliasMessageSource") private val messageSource: MessageSource
) {
  companion object {
    /** Ratio of common nouns to special nouns which are used for rare aliases. */
    private const val NOUN_WEIGHTING = 3
    private val L10N_GENDER = listOf('f', 'm', 'n')
    private val CHOICE_PATTERN = Regex("(f|m|n)#")
  }

  private val adjectives = aliasWordsProperties.adjectives
  private val nouns = aliasWordsProperties.nouns
  private val specialNouns = aliasWordsProperties.specialNouns

  fun generate(locale: Locale): RoomUserAlias =
      generate(ThreadLocalRandom.current().nextInt(), locale)

  fun generate(seed: Int, locale: Locale): RoomUserAlias {
    val seededRandom = Random(seed.toLong())
    val adjectiveKey = "alias-adjectives." + adjectives[seededRandom.nextInt(adjectives.size)]
    val commonNounCount = nouns.size * NOUN_WEIGHTING
    val nounIndex = seededRandom.nextInt(commonNounCount + specialNouns.size)
    val nounKey =
        if (nounIndex >= commonNounCount) {
          "alias-special-nouns." + specialNouns[nounIndex - commonNounCount]
        } else {
          "alias-nouns." + nouns[nounIndex % nouns.size]
        }
    return RoomUserAlias(alias = localize(adjectiveKey, nounKey, locale), seed = seed)
  }

  /**
   * Resolves the words for the locale and adapts the adjective to the noun's grammatical gender.
   * Nouns carry their gender as a `|`-separated suffix which is translated to the index of a choice
   * pattern in the adjective's translation.
   */
  private fun localize(adjectiveKey: String, nounKey: String, locale: Locale): String {
    val noun = messageSource.getMessage(nounKey, null, locale).split("|")
    val genderIndex = if (noun.size > 1) L10N_GENDER.indexOf(noun[1][0]) else -1
    val adjectivePattern =
        CHOICE_PATTERN.replace(messageSource.getMessage(adjectiveKey, null, locale)) {
          L10N_GENDER.indexOf(it.groupValues[1][0]).toString() + "#"
        }
    return MessageFormat.format(adjectivePattern, noun[0], genderIndex)
  }
}
