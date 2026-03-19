/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.Reply

data class UpdateReplyInput(val postId: UUID, val body: String) {
  fun toReply() = Reply(post = Post(id = postId), body = body)
}
