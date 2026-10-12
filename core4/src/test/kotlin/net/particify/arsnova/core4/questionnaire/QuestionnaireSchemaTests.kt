/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNonNull
import net.particify.arsnova.core4.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireSchemaTests {
  @Autowired lateinit var graphQlSource: GraphQlSource

  private fun query(name: String): GraphQLFieldDefinition =
      graphQlSource.schema().queryType.getFieldDefinition(name)

  private fun mutation(name: String): GraphQLFieldDefinition =
      graphQlSource.schema().mutationType.getFieldDefinition(name)

  private fun assertRequired(field: GraphQLFieldDefinition, vararg arguments: String) {
    arguments.forEach {
      Assertions.assertInstanceOf(
          GraphQLNonNull::class.java, field.getArgument(it).type, "${field.name}.$it")
    }
  }

  @Test
  fun shouldRequireTheArgumentsTheResolversCannotDoWithout() {
    assertRequired(mutation("createQuestionnaire"), "input")
    assertRequired(mutation("createQuestionnaireChoiceAnswer"), "input")
    assertRequired(mutation("createQuestionnaireWordcloudAnswer"), "input")
    assertRequired(mutation("createQuestionnaireAbstention"), "roundId")
    assertRequired(mutation("deleteQuestionnaireAnswersByContentId"), "contentId")
    assertRequired(mutation("createQuestionnaireChoiceContent"), "input")
    assertRequired(mutation("createQuestionnaireWordcloudContent"), "input")
    assertRequired(mutation("updateQuestionnaireChoiceContent"), "input")
    assertRequired(mutation("updateQuestionnaireWordcloudContent"), "input")
    assertRequired(mutation("updateQuestionnaireContentOrder"), "questionnaireId", "contentIds")
    assertRequired(mutation("banQuestionnaireWordcloudContentKeyword"), "contentId", "keyword")
    assertRequired(mutation("resetBannedQuestionnaireWordcloudContentKeywords"), "contentId")
  }

  @Test
  fun shouldOfferNoPaginationForContentLists() {
    listOf(
            "questionnaireContentsByQuestionnaireId",
            "questionnairePublishedContentsByQuestionnaireId")
        .forEach {
          Assertions.assertEquals(
              listOf("questionnaireId"), query(it).arguments.map { it.name }, it)
        }
  }
}
