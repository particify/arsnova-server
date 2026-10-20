/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import jakarta.persistence.CascadeType
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.round.Round
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType

@Entity
@Table(schema = "questionnaire")
class Questionnaire(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    var roomId: UUID? = null,
    var name: String? = null,
    @JdbcType(PostgreSQLEnumJdbcType::class) var type: QuestionnaireType? = null,
    var published: Boolean = false,
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var publishingMode: QuestionnairePublishingMode? = QuestionnairePublishingMode.ALL,
    var statisticsPublished: Boolean? = null,
    var correctOptionsPublished: Boolean? = null,
    var leaderboardEnabled: Boolean? = null,
    @OneToMany(mappedBy = "questionnaire", cascade = [CascadeType.ALL])
    @OrderColumn(name = "position")
    var contents: MutableList<Content> = mutableListOf(),
    @ManyToOne var publishedUpToContent: Content? = null,
    @ManyToOne var activeRound: Round? = null,
    @Embedded var auditMetadata: AuditMetadata = AuditMetadata(),
) {
  companion object {
    const val MAX_NAME_LENGTH = 50
  }

  fun isLive(): Boolean = publishingMode == QuestionnairePublishingMode.LIVE

  /** A questionnaire which releases contents one by one starts with its first one released. */
  fun releaseFirstContent(content: Content) {
    if (publishingMode != QuestionnairePublishingMode.ALL && publishedUpToContent == null) {
      publishedUpToContent = content
    }
  }

  /**
   * Stops referencing [content] once it is no longer part of this questionnaire. A published range
   * ending at the content shrinks to the preceding one instead of releasing the following one.
   */
  fun releaseReferencesTo(content: Content) {
    if (activeRound?.content?.id == content.id) activeRound = null
    if (publishedUpToContent?.id == content.id) {
      val index = contents.indexOfFirst { it.id == content.id }
      publishedUpToContent = contents.getOrNull(index - 1)
    }
  }

  /**
   * Extends the released range to [content] if it lies beyond the range's current end. The range
   * never shrinks here, so presenting an earlier content again keeps the later ones released.
   */
  fun releaseUpTo(content: Content) {
    val releasedIndex = contents.indexOfFirst { it.id == publishedUpToContent?.id }
    if (contents.indexOfFirst { it.id == content.id } > releasedIndex) {
      publishedUpToContent = content
    }
  }
}
