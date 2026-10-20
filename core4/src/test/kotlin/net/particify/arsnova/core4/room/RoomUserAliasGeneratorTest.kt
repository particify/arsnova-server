/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.Locale
import net.particify.arsnova.common.util.YamlPropertiesLoader
import net.particify.arsnova.core4.room.internal.AliasWordsProperties
import net.particify.arsnova.core4.room.internal.RoomUserAliasGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.YamlMapFactoryBean
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.ClassPathResource

class RoomUserAliasGeneratorTest {
  companion object {
    private val ENGLISH = Locale.of("en")
    private val GERMAN = Locale.of("de")
    private const val SEED_SWEEP_SIZE = 1000
  }

  private val messageSource = createMessageSource()
  private val aliasWords = loadAliasWords()
  private val generator = RoomUserAliasGenerator(aliasWords, messageSource)

  @Test
  fun shouldGenerateSameAliasForSameSeed() {
    val seed = 42
    val alias = generator.generate(seed, ENGLISH)
    Assertions.assertEquals(seed, alias.seed)
    Assertions.assertEquals(alias.alias, generator.generate(seed, ENGLISH).alias)
  }

  /**
   * Pins seeds to their aliases. Stored aliases are only seeds, so this fails whenever a change to
   * the word lists or the selection would rename existing users.
   */
  @Test
  fun shouldGenerateLocalizedAliases() {
    Assertions.assertEquals("Enthusiastic Owl", generator.generate(0, ENGLISH).alias)
    Assertions.assertEquals("Diligent Donkey", generator.generate(2, ENGLISH).alias)
    Assertions.assertEquals("Focused Sheep", generator.generate(5, ENGLISH).alias)
  }

  @Test
  fun shouldAdaptAdjectivesToGrammaticalGender() {
    Assertions.assertEquals("Enthusiastische Eule", generator.generate(0, GERMAN).alias)
    Assertions.assertEquals("Fleißiger Esel", generator.generate(2, GERMAN).alias)
    Assertions.assertEquals("Fokussiertes Schaf", generator.generate(5, GERMAN).alias)
  }

  @Test
  fun shouldGenerateAliasesForAllSpecialNouns() {
    val specialNouns =
        aliasWords.specialNouns
            .map { messageSource.getMessage("alias-special-nouns.$it", null, ENGLISH) }
            .toSet()
    val generatedAliases = (0..<SEED_SWEEP_SIZE).map { generator.generate(it, ENGLISH).alias }
    val generatedSpecialNouns =
        specialNouns.filter { noun -> generatedAliases.any { it!!.endsWith(" $noun") } }.toSet()
    Assertions.assertEquals(specialNouns, generatedSpecialNouns)
  }

  private fun createMessageSource(): ReloadableResourceBundleMessageSource {
    val messageSource = ReloadableResourceBundleMessageSource()
    messageSource.setBasename("classpath:messages/alias")
    messageSource.setPropertiesPersister(YamlPropertiesLoader())
    messageSource.setFileExtensions(mutableListOf(".yml", ".yaml"))
    messageSource.setFallbackToSystemLocale(false)
    return messageSource
  }

  @Suppress("UNCHECKED_CAST")
  private fun loadAliasWords(): AliasWordsProperties {
    val yamlMapFactory = YamlMapFactoryBean()
    yamlMapFactory.setResources(ClassPathResource("config/alias-words.yaml"))
    val words = yamlMapFactory.getObject()!!["alias-words"] as Map<String, List<String>>
    return AliasWordsProperties(
        adjectives = words["adjectives"]!!,
        nouns = words["nouns"]!!,
        specialNouns = words["special-nouns"]!!)
  }
}
