/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.time.OffsetDateTime
import java.time.ZoneOffset
import net.particify.arsnova.core4.common.TextRenderingService
import net.particify.arsnova.core4.qna.Reply
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class ReplyQueryController(private val textRenderingService: TextRenderingService) {
  @SchemaMapping(typeName = "Reply")
  fun createdAt(reply: Reply): OffsetDateTime? {
    return reply.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "Reply", field = "bodyRendered")
  fun bodyRendered(reply: Reply): String? {
    return textRenderingService.renderText(reply.body)
  }
}
