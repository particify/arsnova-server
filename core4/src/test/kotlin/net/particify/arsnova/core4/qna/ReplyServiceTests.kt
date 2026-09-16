/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.qna.exception.ReplyNotFoundException
import net.particify.arsnova.core4.qna.internal.PostRepository
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import net.particify.arsnova.core4.qna.internal.QnaRepository
import net.particify.arsnova.core4.qna.internal.ReplyRepository
import net.particify.arsnova.core4.qna.internal.ReplyServiceImpl
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Account from the `dev` Liquibase fixtures which owns [ROOM_ID]. */
private val OWNER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

/** Room from the `dev` Liquibase fixtures which each test's Q&A is created in. */
private val ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** Replies publish events, which none of the behaviour covered here depends on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

private const val ANSWER = "Answer"
private const val CORRECTED_ANSWER = "Corrected answer"

/**
 * Covers replies being addressed by their own ID: an edit must update the existing row instead of
 * inserting a second one, and a deletion takes the reply's ID rather than its post's.
 */
@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ReplyServiceTests {
  @Autowired lateinit var postRepository: PostRepository
  @Autowired lateinit var postService: PostServiceImpl
  @Autowired lateinit var qnaRepository: QnaRepository
  @Autowired lateinit var replyRepository: ReplyRepository
  @Autowired lateinit var replyService: ReplyServiceImpl

  private val owner = User(id = OWNER_USER_ID)
  private lateinit var post: Post

  /** Auditing takes the creator from the security context, and `created_by` is NOT NULL. */
  @BeforeEach
  fun createQnaWithPost() {
    SecurityContextHolder.getContext().authentication = authentication(owner)
    val qna = qnaRepository.save(Qna(roomId = ROOM_ID, state = QnaState.STARTED))
    post = postRepository.save(Post(qna = qna, body = "Question"))
  }

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun shouldDeleteReplyById() {
    val created = createReply(ANSWER)
    replyService.delete(checkNotNull(created.id))
    Assertions.assertTrue(replyRepository.findByPostId(postId()).isEmpty())
  }

  /** An unchanged ID is what tells an update apart from the insert it used to be. */
  @Test
  fun shouldNotCreateDuplicateReplyOnEdit() {
    val created = createReply(ANSWER)
    val updated = replyService.update(checkNotNull(created.id), CORRECTED_ANSWER)
    val replies = replyRepository.findByPostId(postId())
    Assertions.assertEquals(1, replies.size)
    Assertions.assertEquals(CORRECTED_ANSWER, replies.single().body)
    Assertions.assertEquals(created.id, replies.single().id)
    Assertions.assertEquals(created.id, updated.id)
  }

  @Test
  fun shouldRejectUpdateOfUnknownReply() {
    assertThrows<ReplyNotFoundException> {
      replyService.update(UUID.randomUUID(), CORRECTED_ANSWER)
    }
  }

  @Test
  fun shouldDeleteReplyWhenPostIsDeleted() {
    createReply(ANSWER)
    postService.delete(postId())
    Assertions.assertTrue(replyRepository.findByPostId(postId()).isEmpty())
  }

  private fun authentication(user: User): Authentication =
      UsernamePasswordAuthenticationToken.authenticated(user, null, listOf())

  private fun createReply(body: String): Reply =
      replyService.create(Reply(post = Post(id = postId()), body = body))

  private fun postId(): UUID = checkNotNull(post.id)
}
