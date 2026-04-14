/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource
import net.particify.arsnova.core4.common.AuditMetadataUuidV7
import net.particify.arsnova.core4.qna.ModerationState
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.Qna
import net.particify.arsnova.core4.qna.QnaState
import net.particify.arsnova.core4.qna.Reply
import net.particify.arsnova.core4.qna.Tag
import net.particify.arsnova.core4.qna.Vote
import net.particify.arsnova.core4.qna.internal.PostRepository
import net.particify.arsnova.core4.qna.internal.QnaRepository
import net.particify.arsnova.core4.qna.internal.VoteRepository
import net.particify.arsnova.core4.room.internal.RoomRepository
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.hibernate.Session
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private const val BATCH_SIZE = 50
/**
 * Qna page size not only affects the Qna alone because related data (posts, votes) is kept in
 * memory for a page.
 */
private const val POST_MIGRATION_QNA_PAGE_SIZE = 20
private const val MIN_THRESHOLD = -50
private const val MAX_THRESHOLD = -5

/** Handles migration of Q&A-related data from v3. */
@Component
@ConditionalOnBooleanProperty(name = ["persistence.v3-migration.enabled"])
class QnaMigrator(
    @PersistenceContext val entityManager: EntityManager,
    @Qualifier("legacyQnaDataSource") dataSource: DataSource,
    val couchdbMigrator: CouchdbMigrator,
    val userService: UserServiceImpl,
    val roomRepository: RoomRepository,
    val qnaRepository: QnaRepository,
    val postRepository: PostRepository,
    val voteRepository: VoteRepository
) {
  private val logger = LoggerFactory.getLogger(QnaMigrator::class.java)
  private val jdbcClient = JdbcClient.create(dataSource)
  private val randomGenerator = SecureRandom()

  @Transactional
  fun migrateSettings() {
    logger.info("Migrating Comment settings...")
    val count = qnaRepository.count()
    if (count > 0) {
      logger.warn("Qna table not empty ({}), skipping migration.", count)
      return
    }
    entityManager.unwrap<Session>(Session::class.java).jdbcBatchSize = JDBC_BATCH_SIZE
    var page = 0
    do {
      val rooms = roomRepository.findAll(PageRequest.of(page, BATCH_SIZE))
      for (room in rooms) {
        val settings =
            jdbcClient
                .sql(
                    "SELECT disabled, readonly, direct_send " +
                        "FROM settings " +
                        "WHERE room_id = :id")
                .param("id", room.id)
                .query(QnaCommentSettingsRowMapper())
                .optional()
                .orElse(QnaCommentSettingsRowMapper.CommentSettings())
        val qna =
            Qna(
                roomId = room.id,
                topic = "Q&A",
                state = determineState(settings.disabled, settings.readonly),
                autoPublish = settings.directSend)
        qna.auditMetadata.createdAt = room.auditMetadata.createdAt
        qna.auditMetadata.createdBy = room.auditMetadata.createdBy
        logger.trace("Migrating comment settings of Room {} for Qna...", qna.roomId)
        entityManager.persist(qna)
      }
      val flushStart = Instant.now()
      entityManager.flush()
      entityManager.clear()
      logger.debug("Flush took {} ms.", flushStart.until(Instant.now(), ChronoUnit.MILLIS))
      page++
    } while (!rooms.isEmpty)
    migrateAdditionalSettings()
  }

  @Transactional
  fun migratePostsAndRelatedData() {
    logger.info("Migrating Comment data...")
    val count = postRepository.count()
    if (count > 0) {
      logger.warn("Post table not empty ({}), skipping migration.", count)
      return
    }
    val migrationStart = Instant.now()
    entityManager.unwrap<Session>(Session::class.java).jdbcBatchSize = JDBC_BATCH_SIZE
    var page = 0
    do {
      val qnas = qnaRepository.findAll(PageRequest.of(page, POST_MIGRATION_QNA_PAGE_SIZE))
      for (qna in qnas) {
        logger.trace("Migrating Comment data for Room {}...", qna.roomId)
        val comments =
            jdbcClient
                .sql(
                    "SELECT id, body, answer, ack, correct, favorite, tag, creator_id, timestamp " +
                        "FROM comment " +
                        "WHERE room_id = :roomId")
                .param("roomId", qna.roomId!!)
                .query(QnaCommentRowMapper())
                .list()
        var missingTagCount = 0
        for (comment in comments) {
          if (migrateComment(qna, comment).missingTag) {
            missingTagCount++
          }
        }
        if (missingTagCount > 0) {
          logger.warn("Tags missing for {} posts of qna {}.", missingTagCount, qna.id)
        }
        val flushStart = Instant.now()
        entityManager.flush()
        logger.debug("Flush took {} ms.", flushStart.until(Instant.now(), ChronoUnit.MILLIS))
      }
      entityManager.clear()
      page++
    } while (!qnas.isEmpty)
    logger.info(
        "Migration of Comment data completed in {} seconds.",
        migrationStart.until(Instant.now(), ChronoUnit.SECONDS))
  }

  private fun migrateComment(
      qna: Qna,
      comment: QnaCommentRowMapper.Comment
  ): CommentMigrationResult {
    val ghostUser = userService.getOrCreateGhostUser()
    val creatorId =
        if (comment.creatorId == UuidHelper.NIL || !userService.existsById(comment.creatorId))
            ghostUser.id
        else comment.creatorId
    val post =
        Post(
            id = UuidHelper.generateUuidV7(comment.timestamp, randomGenerator),
            qna = qna,
            body = comment.body,
            correct =
                when (comment.correct) {
                  1 -> true
                  2 -> false
                  else -> null
                },
            favorite = comment.favorite,
            moderationState =
                if (comment.ack) ModerationState.ACCEPTED else ModerationState.REJECTED,
            auditMetadata = AuditMetadataUuidV7(createdBy = creatorId))
    val tag = qna.tags.find { it.name == comment.tag?.trim() }
    var missingTag = false
    if (tag != null) {
      post.tags.add(tag)
    } else if (!comment.tag.isNullOrBlank()) {
      missingTag = true
    }
    entityManager.persist(post)
    if (!comment.answer.isNullOrBlank()) {
      val reply =
          Reply(
              id = UuidHelper.generateUuidV7(comment.timestamp, randomGenerator),
              post = post,
              body = comment.answer.trim(),
              auditMetadata = AuditMetadataUuidV7(createdBy = ghostUser.id),
          )
      entityManager.persist(reply)
    }
    migrateVotes(post, comment.id)
    logger.trace("Migrating Comment {} -> {}...", comment.id, post.id)
    return CommentMigrationResult(missingTag)
  }

  private data class CommentMigrationResult(val missingTag: Boolean)

  @Transactional
  private fun migrateVotes(post: Post, commentId: UUID) {
    logger.trace("Migrating Vote data...")
    val ghostUser = userService.getOrCreateGhostUser()
    val votes =
        jdbcClient
            .sql("SELECT user_id, vote " + "FROM vote " + "WHERE comment_id = :commentId")
            .param("commentId", commentId)
            .query(QnaVoteRowMapper())
            .list()
    for (vote in votes) {
      val userId = vote.userId
      var newVote: Vote?
      if (userId == UuidHelper.NIL || !userService.existsById(userId)) {
        // User might have been deleted or is surrogate (in case of demo rooms).
        logger.debug("User {} for vote not found. Using ghost user.", userId)
        newVote = voteRepository.findByIdOrNull(Vote.PostUserId(post.id!!, ghostUser.id))
        if (newVote == null) {
          newVote = Vote(post = post, user = ghostUser, value = vote.vote)
        } else {
          logger.debug("Vote already exists (postId: {}, userId: {}).", post.id, newVote.user?.id)
          newVote.value += vote.vote
        }
      } else {
        val userRef = userService.getReferenceById(userId)
        newVote = Vote(post = post, user = userRef, value = vote.vote)
      }
      logger.trace("Migrating Vote (postId: {}, userId: {})...", post.id, newVote.user?.id)
      entityManager.persist(newVote)
    }
  }

  private fun determineState(disabled: Boolean, readonly: Boolean) =
      if (disabled) QnaState.STOPPED else if (readonly) QnaState.PAUSED else QnaState.STARTED

  private fun migrateAdditionalSettings() {
    couchdbMigrator.migrate<RoomSettings> {
      if (!it.commentThresholdEnabled && it.commentTags.isEmpty()) {
        return@migrate null
      }
      val roomId = UuidHelper.stringToUuid(it.roomId)!!
      val qna = qnaRepository.findFirstByRoomId(roomId)
      if (qna == null) {
        logger.warn("Room {} does not exist. Skipping qna settings update.", roomId)
        return@migrate null
      }
      logger.trace("Migrating additional qna settings for room {}...", roomId)
      if (it.commentThresholdEnabled) {
        qna.threshold = it.commentThreshold.coerceAtLeast(MIN_THRESHOLD).coerceAtMost(MAX_THRESHOLD)
      }
      val tags = it.commentTags.map { tag -> tag.trim() }.filter { tag -> tag.isNotBlank() }
      if (tags.isNotEmpty()) {
        qna.tags.addAll(tags.map { name -> Tag(qna = qna, name = name) })
      }
      return@migrate listOf(qna).plus(qna.tags)
    }
  }
}
