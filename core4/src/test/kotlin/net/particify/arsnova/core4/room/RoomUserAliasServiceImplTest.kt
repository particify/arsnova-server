/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.common.util.YamlPropertiesLoader
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.room.internal.AliasWordsProperties
import net.particify.arsnova.core4.room.internal.RoomUserAliasGenerator
import net.particify.arsnova.core4.room.internal.RoomUserAliasRepository
import net.particify.arsnova.core4.room.internal.RoomUserAliasServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.config.YamlMapFactoryBean
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.ClassPathResource
import org.springframework.dao.DataIntegrityViolationException

private val ROOM_ID = UUID.randomUUID()
private val USER_ID = UUID.randomUUID()
private val ENGLISH = Locale.of("en")
private val GERMAN = Locale.of("de")

// Seeds whose aliases RoomUserAliasGeneratorTest pins.
private const val DONKEY_SEED = 2

class RoomUserAliasServiceImplTest {
  private val repository = Mockito.mock(RoomUserAliasRepository::class.java)
  private val service =
      RoomUserAliasServiceImpl(
          repository,
          RoomUserAliasGenerator(loadAliasWords(), messageSource()),
          RoomUserAliasEventPublisher())

  init {
    Mockito.`when`(repository.saveAndFlush(Mockito.any(RoomUserAlias::class.java))).thenAnswer {
      (it.arguments[0] as RoomUserAlias).also { alias -> alias.id = UUID.randomUUID() }
    }
  }

  private fun givenStored(alias: RoomUserAlias?) {
    Mockito.`when`(repository.findOneByRoomIdAndUserId(ROOM_ID, USER_ID)).thenReturn(alias)
  }

  @Test
  fun shouldProposeAnUnpersistedGeneratedAliasWhileNoneIsSet() {
    givenStored(null)
    val proposal = service.retrieveOrGenerate(ROOM_ID, USER_ID, ENGLISH)
    Assertions.assertNull(proposal.id)
    Assertions.assertNotNull(proposal.seed)
    Assertions.assertFalse(proposal.alias.isNullOrBlank())
    Mockito.verify(repository, Mockito.never()).saveAndFlush(Mockito.any())
  }

  @Test
  fun shouldReturnAnExplicitlySetAliasAsStored() {
    val stored = RoomUserAlias(id = UUID.randomUUID(), alias = "Captain", seed = DONKEY_SEED)
    givenStored(stored)
    Assertions.assertSame(stored, service.retrieveOrGenerate(ROOM_ID, USER_ID, GERMAN))
  }

  @Test
  fun shouldLocalizeAStoredGeneratedAliasPerRequest() {
    val stored = RoomUserAlias(id = UUID.randomUUID(), seed = DONKEY_SEED)
    givenStored(stored)
    val english = service.retrieveOrGenerate(ROOM_ID, USER_ID, ENGLISH)
    val german = service.retrieveOrGenerate(ROOM_ID, USER_ID, GERMAN)
    Assertions.assertEquals("Diligent Donkey", english.alias)
    Assertions.assertEquals("Fleißiger Esel", german.alias)
    Assertions.assertEquals(listOf(stored.id, stored.id), listOf(english.id, german.id))
    Assertions.assertEquals(DONKEY_SEED, german.seed)
    Assertions.assertNull(stored.alias)
  }

  @Test
  fun shouldRejectASecondAlias() {
    givenStored(RoomUserAlias(id = UUID.randomUUID(), seed = DONKEY_SEED))
    Assertions.assertThrows(InvalidInputException::class.java) {
      service.set(ROOM_ID, USER_ID, "Captain", null, ENGLISH)
    }
    Mockito.verify(repository, Mockito.never()).saveAndFlush(Mockito.any())
  }

  @Test
  fun shouldRejectAnAliasCreatedConcurrently() {
    givenStored(null)
    Mockito.doThrow(DataIntegrityViolationException("user_alias_room_id_user_id_key"))
        .`when`(repository)
        .saveAndFlush(Mockito.any(RoomUserAlias::class.java))
    Assertions.assertThrows(InvalidInputException::class.java) {
      service.set(ROOM_ID, USER_ID, "Captain", null, ENGLISH)
    }
  }

  @Test
  fun shouldRejectABlankOrOverlongAlias() {
    givenStored(null)
    listOf("", "  ", "x".repeat(RoomUserAlias.MAX_ALIAS_LENGTH + 1)).forEach {
      Assertions.assertThrows(
          InvalidInputException::class.java,
          { service.set(ROOM_ID, USER_ID, it, null, ENGLISH) },
          it)
    }
    Mockito.verify(repository, Mockito.never()).saveAndFlush(Mockito.any())
  }

  @Test
  fun shouldAcceptAnAliasOfMaximumLength() {
    givenStored(null)
    val longest = "x".repeat(RoomUserAlias.MAX_ALIAS_LENGTH)
    Assertions.assertEquals(longest, service.set(ROOM_ID, USER_ID, longest, null, ENGLISH).alias)
  }

  @Test
  fun shouldTrimAnAliasBeforeCheckingItsLength() {
    givenStored(null)
    val longest = "x".repeat(RoomUserAlias.MAX_ALIAS_LENGTH)
    Assertions.assertEquals(
        longest, service.set(ROOM_ID, USER_ID, " \t$longest\n ", null, ENGLISH).alias)
    val captor = ArgumentCaptor.forClass(RoomUserAlias::class.java)
    Mockito.verify(repository).saveAndFlush(captor.capture())
    Assertions.assertEquals(longest, captor.value.alias)
  }

  @Test
  fun shouldPersistAnExplicitAliasForTheRoomAndUser() {
    givenStored(null)
    val set = service.set(ROOM_ID, USER_ID, "Captain", null, ENGLISH)
    Assertions.assertEquals("Captain", set.alias)
    Assertions.assertNotNull(set.seed)
    Assertions.assertEquals(ROOM_ID, set.room?.id)
    Assertions.assertEquals(USER_ID, set.user?.id)
  }

  @Test
  fun shouldPersistOnlyTheSeedOfAGeneratedAliasAndReturnItLocalized() {
    givenStored(null)
    val set = service.set(ROOM_ID, USER_ID, null, DONKEY_SEED, GERMAN)
    Assertions.assertEquals("Fleißiger Esel", set.alias)
    Assertions.assertNotNull(set.id)
    val captor = ArgumentCaptor.forClass(RoomUserAlias::class.java)
    Mockito.verify(repository).saveAndFlush(captor.capture())
    Assertions.assertNull(captor.value.alias)
    Assertions.assertEquals(DONKEY_SEED, captor.value.seed)
    Assertions.assertEquals(captor.value.id, set.id)
  }

  @Test
  fun shouldDrawASeedWhenNoneIsGiven() {
    givenStored(null)
    val set = service.set(ROOM_ID, USER_ID, null, null, ENGLISH)
    Assertions.assertNotNull(set.seed)
    Assertions.assertFalse(set.alias.isNullOrBlank())
  }

  private fun messageSource(): ReloadableResourceBundleMessageSource {
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
