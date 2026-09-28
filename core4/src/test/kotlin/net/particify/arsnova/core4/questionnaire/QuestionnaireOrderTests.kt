/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireOrderTests {
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    val user = userService.loadUserById(USER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  /** The room is shared with other tests, so only the questionnaires created here are compared. */
  private fun namesOf(questionnaires: List<Questionnaire>, prefix: String) =
      questionnaires.mapNotNull { it.name }.filter { it.startsWith(prefix) }

  private fun createAll(prefix: String, suffixes: List<String>) =
      suffixes.map { questionnaireService.create(ROOM_ID, prefix + it, QuestionnaireType.SURVEY) }

  @Test
  fun shouldOrderByNameIgnoringCase() {
    val prefix = "Order ${UUID.randomUUID()} "
    createAll(prefix, listOf("b", "C", "a"))
    val names = namesOf(questionnaireService.findByRoomIdOrderByName(ROOM_ID), prefix)
    Assertions.assertEquals(listOf("a", "b", "C").map { prefix + it }, names)
  }

  @Test
  fun shouldOrderPublishedByNameIgnoringCase() {
    val prefix = "Published ${UUID.randomUUID()} "
    val created = createAll(prefix, listOf("b", "C", "a", "d"))
    created
        .filter { it.name != prefix + "d" }
        .forEach { questionnaireService.updatePublished(it.id!!, true) }
    val names = namesOf(questionnaireService.findPublishedByRoomIdOrderByName(ROOM_ID), prefix)
    Assertions.assertEquals(listOf("a", "b", "C").map { prefix + it }, names)
  }
}
