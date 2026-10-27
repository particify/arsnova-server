/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.common.AuditMetadataUuidV7
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.executeAsCurrentUser
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.template.Template
import net.particify.arsnova.core4.questionnaire.template.TemplateRepository
import net.particify.arsnova.core4.questionnaire.template.api.CreateTemplateFromQuestionnaireInput
import net.particify.arsnova.core4.questionnaire.template.api.TemplateMutationController
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.UserService
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision
import net.particify.arsnova.core4.violationreport.internal.ViolationReportRepository
import net.particify.arsnova.core4.violationreport.internal.api.CreateViolationReportInput
import net.particify.arsnova.core4.violationreport.internal.api.ViolationReportMutationController
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.ExecutionGraphQlResponse
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.execution.ErrorType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Accounts from the `dev` Liquibase fixtures, neither of them an admin. */
private val CREATOR_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val REPORTER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

private const val ROOM = "ROOM"
private const val TEMPLATE = "TEMPLATE"

/** Room events reach RabbitMQ, which none of this depends on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

private enum class Access {
  GUEST,
  VERIFIED,
  ADMIN
}

/**
 * Other test classes share the schema, so these tests assert on the reports they created and on
 * changes of the statistics, never on totals.
 */
@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ViolationReportTests {
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
  @Autowired lateinit var membershipService: MembershipServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roomService: RoomServiceImpl
  @Autowired lateinit var templateMutationController: TemplateMutationController
  @Autowired lateinit var templateRepository: TemplateRepository
  @Autowired lateinit var userService: UserService
  @Autowired lateinit var violationReportMutationController: ViolationReportMutationController
  @Autowired lateinit var violationReportRepository: ViolationReportRepository

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(userId: UUID, access: Access = Access.VERIFIED) {
    val user = userService.loadUserById(userId)!!
    val extraRoles =
        when (access) {
          Access.GUEST -> listOf()
          Access.VERIFIED -> listOf("ROLE_VERIFIED")
          Access.ADMIN -> listOf("ROLE_VERIFIED", "ROLE_ADMIN")
        }
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            user, null, user.authorities + extraRoles.map(::SimpleGrantedAuthority))
  }

  private fun createRoom(): Room {
    authenticate(CREATOR_ID)
    val user = userService.loadUserById(CREATOR_ID)!!
    return roomService.create(
        Room(shortId = roomService.generateShortId(), name = "Reported"), user)
  }

  /** Only members may read, and so report, a room. */
  private fun createRoomJoinedByReporter(): Room {
    val room = createRoom()
    membershipService.joinRoom(room, userService.loadUserById(REPORTER_ID)!!)
    return room
  }

  private fun createTemplate(published: Boolean): Template {
    val room = createRoom()
    val questionnaire = questionnaireService.create(room.id!!, "Reported", QuestionnaireType.SURVEY)
    contentService.create(
        questionnaire.id!!,
        "Pick one",
        ChoiceContentFormatData(listOf("A", "B"), null, false),
        null,
        true,
        null)
    return templateMutationController.createTemplateFromQuestionnaire(
        CreateTemplateFromQuestionnaireInput(
            aiGenerated = false,
            attribution = null,
            description = null,
            language = "en",
            licenseId = "CC0-1.0",
            name = "Reported template",
            published = published,
            questionnaireId = questionnaire.id!!,
            tagNames = listOf()))
  }

  private fun execute(document: String): ExecutionGraphQlResponse =
      graphQlService.executeAsCurrentUser(document.trimIndent())

  private fun createReport(
      targetType: String,
      targetId: UUID,
      reason: ViolationReportReason = ViolationReportReason.COPYRIGHT
  ): ExecutionGraphQlResponse =
      execute(
          """
          mutation {
            createViolationReport(input: {
              targetType: $targetType
              targetId: "$targetId"
              reason: $reason
              description: " Copied from elsewhere "
            })
          }
          """)

  private fun createdReportId(response: ExecutionGraphQlResponse): UUID {
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return UUID.fromString(response.field("createViolationReport").getValue<String>())
  }

  private fun decide(id: UUID, decision: ViolationReportDecision): ExecutionGraphQlResponse =
      execute(
          """
          mutation {
            adminDecideViolationReport(id: "$id", decision: $decision) {
              id
              decision
              updatedBy
              updatedAt
            }
          }
          """)

  private fun listedReportIds(decided: Boolean): List<UUID> {
    val response =
        execute(
            """
            {
              adminViolationReportsByDecided(decided: $decided) {
                edges {
                  node {
                    id
                    targetType
                    targetId
                    reason
                    description
                    decision
                    createdBy
                    createdAt
                  }
                }
              }
            }
            """)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return response
        .field("adminViolationReportsByDecided.edges")
        .getValue<List<Map<String, Map<String, Any?>>>>()!!
        .map { UUID.fromString(it.getValue("node").getValue("id") as String) }
  }

  private fun stats(): Map<String, Any?> {
    val response =
        execute(
            """
            {
              adminViolationReportStats {
                totalCount
                undecidedCount
                countsByReason {
                  reason
                  count
                }
                countsByDecision {
                  decision
                  count
                }
              }
            }
            """)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return response.field("adminViolationReportStats").getValue<Map<String, Any?>>()!!
  }

  private fun countOf(stats: Map<String, Any?>, list: String, key: String, value: String): Int {
    @Suppress("UNCHECKED_CAST") val entries = stats.getValue(list) as List<Map<String, Any?>>
    return entries.find { it[key] == value }?.get("count") as Int? ?: 0
  }

  private fun assertErrorType(expected: ErrorType, response: ExecutionGraphQlResponse) {
    Assertions.assertEquals(listOf(expected), response.errors.map { it.errorType }, "$response")
  }

  @Test
  fun shouldStoreReportOfVerifiedUserWithoutExposingIt() {
    val template = createTemplate(published = true)
    authenticate(REPORTER_ID)
    val id = createdReportId(createReport(TEMPLATE, template.id!!))

    val report = violationReportRepository.findByIdOrNull(id)!!
    Assertions.assertEquals(template.id, report.targetId)
    Assertions.assertEquals(TEMPLATE, report.targetType)
    Assertions.assertEquals("Copied from elsewhere", report.description)
    Assertions.assertEquals(REPORTER_ID, report.auditMetadata.createdBy)
    Assertions.assertNull(report.decision)
    assertErrorType(ErrorType.FORBIDDEN, decide(id, ViolationReportDecision.INVALID))
    val list =
        execute("{ adminViolationReportsByDecided(decided: false) { edges { node { id } } } }")
    assertErrorType(ErrorType.FORBIDDEN, list)
  }

  /** The GraphQL enum rejects an unregistered type already, so this calls the controller. */
  @Test
  fun shouldRejectUnregisteredTargetTypeAsInvalidInput() {
    authenticate(REPORTER_ID)
    val input =
        CreateViolationReportInput(
            targetType = "UNREGISTERED",
            targetId = UUID.randomUUID(),
            reason = ViolationReportReason.OTHER,
            description = "Unknown")
    assertThrows<InvalidInputException> {
      violationReportMutationController.createViolationReport(input)
    }
  }

  /** Introspection counts one `__type` per request as in good faith. */
  private fun introspect(typeName: String, selection: String): Map<String, Any?> {
    val response = execute("""{ __type(name: "$typeName") { $selection } }""")
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return response.field("__type").getValue<Map<String, Any?>>()!!
  }

  /** The name of the type wrapped as non-null by the `targetType` field in [fields]. */
  @Suppress("UNCHECKED_CAST")
  private fun targetTypeFieldType(typeName: String, fields: String): String? {
    val selection = "$fields { name type { ofType { name } } }"
    val field =
        (introspect(typeName, selection).getValue(fields) as List<Map<String, Any?>>).single {
          it["name"] == "targetType"
        }
    return ((field.getValue("type") as Map<String, Any?>).getValue("ofType") as Map<String, Any?>)[
        "name"]
        as String?
  }

  @Test
  fun shouldExposeTargetTypesAsEnum() {
    authenticate(REPORTER_ID)
    val enumType = introspect("ViolationReportTargetType", "kind enumValues { name }")
    Assertions.assertEquals("ENUM", enumType["kind"])
    @Suppress("UNCHECKED_CAST")
    val values = (enumType.getValue("enumValues") as List<Map<String, Any?>>).map { it["name"] }
    // The order of extended values follows the loading order of the schema files.
    Assertions.assertEquals(setOf(ROOM, TEMPLATE), values.toSet())
    Assertions.assertEquals(
        "ViolationReportTargetType", targetTypeFieldType("ViolationReport", "fields"))
    Assertions.assertEquals(
        "ViolationReportTargetType",
        targetTypeFieldType("CreateViolationReportInput", "inputFields"))
  }

  @Test
  fun shouldRejectReportOfGuest() {
    val room = createRoomJoinedByReporter()
    authenticate(REPORTER_ID, Access.GUEST)
    assertErrorType(ErrorType.FORBIDDEN, createReport(ROOM, room.id!!))
  }

  @Test
  fun shouldLetOnlyTheCreatorReportAnUnpublishedTemplate() {
    val template = createTemplate(published = false)
    authenticate(REPORTER_ID)
    assertErrorType(ErrorType.FORBIDDEN, createReport(TEMPLATE, template.id!!))
    authenticate(CREATOR_ID)
    createdReportId(createReport(TEMPLATE, template.id!!))
  }

  @Test
  fun shouldListReportsByDecidedNewestFirst() {
    val room = createRoomJoinedByReporter()
    authenticate(REPORTER_ID)
    val older = createdReportId(createReport(ROOM, room.id!!))
    val newer = createdReportId(createReport(ROOM, room.id!!))
    authenticate(CREATOR_ID, Access.ADMIN)

    val undecided = listedReportIds(decided = false)
    Assertions.assertTrue(undecided.indexOf(newer) in 0..<undecided.indexOf(older), "$undecided")
    val response = decide(older, ViolationReportDecision.INVALID)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    Assertions.assertFalse(listedReportIds(decided = false).contains(older))
    Assertions.assertTrue(listedReportIds(decided = true).contains(older))
    Assertions.assertTrue(listedReportIds(decided = false).contains(newer))
    Assertions.assertTrue(roomService.existsById(room.id!!))
  }

  @Test
  fun shouldKeepTheFirstDecision() {
    val template = createTemplate(published = true)
    authenticate(REPORTER_ID)
    val id = createdReportId(createReport(TEMPLATE, template.id!!))
    authenticate(CREATOR_ID, Access.ADMIN)

    val response = decide(id, ViolationReportDecision.INVALID)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    Assertions.assertEquals(
        CREATOR_ID.toString(),
        response.field("adminDecideViolationReport.updatedBy").getValue<String>())
    assertErrorType(ErrorType.BAD_REQUEST, decide(id, ViolationReportDecision.REMOVAL))
    Assertions.assertEquals(
        ViolationReportDecision.INVALID, violationReportRepository.findByIdOrNull(id)!!.decision)
    Assertions.assertNotNull(templateRepository.findByIdOrNull(template.id!!))
  }

  @Test
  fun shouldDeleteTemplateAndCloseOtherReportsOnRemoval() {
    val template = createTemplate(published = true)
    val other = createTemplate(published = true)
    authenticate(REPORTER_ID)
    val id = createdReportId(createReport(TEMPLATE, template.id!!))
    val sibling = createdReportId(createReport(TEMPLATE, template.id!!))
    val unrelated = createdReportId(createReport(TEMPLATE, other.id!!))
    authenticate(CREATOR_ID, Access.ADMIN)

    val response = decide(id, ViolationReportDecision.REMOVAL)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    Assertions.assertNull(templateRepository.findByIdOrNull(template.id!!))
    Assertions.assertNotNull(templateRepository.findByIdOrNull(other.id!!))
    val closed = violationReportRepository.findByIdOrNull(sibling)!!
    Assertions.assertEquals(ViolationReportDecision.REMOVAL, closed.decision)
    Assertions.assertEquals(CREATOR_ID, closed.auditMetadata.updatedBy)
    Assertions.assertNull(violationReportRepository.findByIdOrNull(unrelated)!!.decision)
  }

  @Test
  fun shouldDeleteRoomOnRemoval() {
    val room = createRoomJoinedByReporter()
    authenticate(REPORTER_ID)
    val id = createdReportId(createReport(ROOM, room.id!!))
    authenticate(CREATOR_ID, Access.ADMIN)

    val response = decide(id, ViolationReportDecision.REMOVAL)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    Assertions.assertFalse(roomService.existsById(room.id!!))
    Assertions.assertNotNull(violationReportRepository.findByIdOrNull(id))
  }

  @Test
  fun shouldDecideRemovalOfMissingTarget() {
    val report =
        violationReportRepository.save(
            ViolationReport(
                targetType = TEMPLATE,
                targetId = UUID.randomUUID(),
                reason = ViolationReportReason.OTHER,
                description = "Gone",
                auditMetadata = AuditMetadataUuidV7(createdBy = REPORTER_ID)))
    authenticate(CREATOR_ID, Access.ADMIN)

    val response = decide(report.id!!, ViolationReportDecision.REMOVAL)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
  }

  @Test
  fun shouldCountReports() {
    val room = createRoomJoinedByReporter()
    authenticate(CREATOR_ID, Access.ADMIN)
    val before = stats()
    authenticate(REPORTER_ID)
    val id = createdReportId(createReport(ROOM, room.id!!, ViolationReportReason.ADVERTISING))
    createdReportId(createReport(ROOM, room.id!!))
    authenticate(CREATOR_ID, Access.ADMIN)
    val response = decide(id, ViolationReportDecision.INVALID)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())

    val after = stats()
    val total = "totalCount"
    val undecided = "undecidedCount"
    Assertions.assertEquals(before.getValue(total) as Int + 2, after.getValue(total))
    Assertions.assertEquals(before.getValue(undecided) as Int + 1, after.getValue(undecided))
    val byReason = "countsByReason"
    Assertions.assertEquals(
        countOf(before, byReason, "reason", "ADVERTISING") + 1,
        countOf(after, byReason, "reason", "ADVERTISING"))
    val byDecision = "countsByDecision"
    Assertions.assertEquals(
        countOf(before, byDecision, "decision", "INVALID") + 1,
        countOf(after, byDecision, "decision", "INVALID"))
    Assertions.assertEquals(
        countOf(before, byDecision, "decision", "REMOVAL"),
        countOf(after, byDecision, "decision", "REMOVAL"))
  }
}
