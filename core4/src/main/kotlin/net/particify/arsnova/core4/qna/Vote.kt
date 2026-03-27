/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.util.UUID
import net.particify.arsnova.core4.user.User

@Entity
@Table(schema = "qna")
class Vote(
    @EmbeddedId var id: PostUserId? = PostUserId(),
    @ManyToOne @MapsId("postId") var post: Post? = null,
    @ManyToOne @MapsId("userId") var user: User? = null,
    var value: Int = 0,
) {
  @Embeddable data class PostUserId(var postId: UUID? = null, var userId: UUID? = null)

  fun copy(postId: UUID, userId: UUID): Vote {
    return Vote(post = Post(id = postId), user = User(id = userId), value = value)
  }
}
