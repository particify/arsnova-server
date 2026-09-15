/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.qna.internal.PostRepository
import net.particify.arsnova.core4.qna.internal.QnaRepository
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.system.security.DelegatingPermissionEvaluator
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Account from the `dev` Liquibase fixtures which owns [ROOM_ID]. */
private val OWNER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** Account creation publishes an event, which permission evaluation does not depend on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

private const val CREATE_POST = "create_post"
private const val DELETE = "delete"
private const val MODERATE = "moderate"
private const val POST = "Post"
private const val QNA = "Qna"
private const val READ = "read"

/**
 * Covers the Post -> Qna -> Room chain the delegating evaluator walks, because what a role may
 * moderate is decided by all three evaluators together rather than by any one of them.
 */
@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QnaModerationPermissionTests {
  @Autowired lateinit var permissionEvaluator: DelegatingPermissionEvaluator
  @Autowired lateinit var qnaRepository: QnaRepository
  @Autowired lateinit var postRepository: PostRepository
  @Autowired lateinit var membershipService: MembershipServiceImpl
  @Autowired lateinit var userService: UserService

  private val owner = User(id = OWNER_USER_ID)
  private lateinit var qna: Qna
  private lateinit var post: Post

  /** Auditing takes the creator from the security context, so the owner is authenticated here. */
  @BeforeEach
  fun createQnaWithPost() {
    SecurityContextHolder.getContext().authentication = authentication(owner)
    qna = qnaRepository.save(Qna(roomId = ROOM_ID, state = QnaState.STARTED))
    post = postRepository.save(Post(qna = qna, body = "Question"))
  }

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun shouldGrantModerationToModerator() {
    val user = member(RoomRole.MODERATOR)
    Assertions.assertTrue(hasPermission(user, QNA, qna.id, MODERATE), QNA)
    Assertions.assertTrue(hasPermission(user, POST, post.id, MODERATE), POST)
  }

  @Test
  fun shouldGrantModerationToEditor() {
    val user = member(RoomRole.EDITOR)
    Assertions.assertTrue(hasPermission(user, QNA, qna.id, MODERATE), QNA)
    Assertions.assertTrue(hasPermission(user, POST, post.id, MODERATE), POST)
  }

  @Test
  fun shouldDenyModerationToParticipant() {
    val user = member(RoomRole.PARTICIPANT)
    Assertions.assertFalse(hasPermission(user, QNA, qna.id, MODERATE), QNA)
    Assertions.assertFalse(hasPermission(user, POST, post.id, MODERATE), POST)
  }

  /**
   * A reply is v3's answer to a post, and clearing one was part of answering it, so
   * `deleteQnaReply` is gated on the post's `moderate` rather than on its `delete`.
   */
  @Test
  fun shouldGrantReplyDeletionToModerator() {
    Assertions.assertTrue(hasPermission(member(RoomRole.MODERATOR), POST, post.id, MODERATE))
  }

  @Test
  fun shouldDenyReplyDeletionToParticipant() {
    Assertions.assertFalse(hasPermission(member(RoomRole.PARTICIPANT), POST, post.id, MODERATE))
  }

  /** Post deletion stayed with the roles which had it in v3; moderators were not among them. */
  @Test
  fun shouldDenyDeletionToModerator() {
    val user = member(RoomRole.MODERATOR)
    Assertions.assertFalse(hasPermission(user, QNA, qna.id, DELETE), QNA)
    Assertions.assertFalse(hasPermission(user, POST, post.id, DELETE), POST)
  }

  @Test
  fun shouldGrantDeletionToOwner() {
    Assertions.assertTrue(hasPermission(owner, QNA, qna.id, DELETE), QNA)
    Assertions.assertTrue(hasPermission(owner, POST, post.id, DELETE), POST)
  }

  /** A stopped Q&A falls back to `moderate`, which is what keeps moderators from losing access. */
  @Test
  fun shouldGrantAccessToStoppedQnaForModerator() {
    val user = member(RoomRole.MODERATOR)
    stopQna()
    Assertions.assertTrue(hasPermission(user, QNA, qna.id, READ), QNA)
    Assertions.assertTrue(hasPermission(user, POST, post.id, READ), POST)
    Assertions.assertTrue(hasPermission(user, QNA, qna.id, CREATE_POST), CREATE_POST)
  }

  @Test
  fun shouldDenyAccessToStoppedQnaForParticipant() {
    val user = member(RoomRole.PARTICIPANT)
    stopQna()
    Assertions.assertFalse(hasPermission(user, QNA, qna.id, READ), QNA)
    Assertions.assertFalse(hasPermission(user, POST, post.id, READ), POST)
    Assertions.assertFalse(hasPermission(user, QNA, qna.id, CREATE_POST), CREATE_POST)
  }

  private fun stopQna() {
    SecurityContextHolder.getContext().authentication = authentication(owner)
    qna.state = QnaState.STOPPED
    qna = qnaRepository.save(qna)
  }

  private fun hasPermission(
      user: User,
      targetType: String,
      targetId: UUID?,
      permission: String
  ): Boolean =
      permissionEvaluator.hasPermission(
          authentication(user), checkNotNull(targetId), targetType, permission)

  private fun authentication(user: User): Authentication =
      UsernamePasswordAuthenticationToken.authenticated(user, null, listOf())

  private fun member(role: RoomRole): User {
    val user = userService.createAccount()
    membershipService.save(
        Membership(room = Room(id = ROOM_ID), user = User(id = user.id), role = role))
    return user
  }
}
