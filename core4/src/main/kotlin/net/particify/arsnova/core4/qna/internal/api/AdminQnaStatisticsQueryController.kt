/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import net.particify.arsnova.core4.qna.AdminQnaStats
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminQnaStatisticsQueryController(
    private val postServiceImpl: PostServiceImpl,
) {

  @QueryMapping
  fun adminQnaStats(): AdminQnaStats {
    return AdminQnaStats(
        postCount = postServiceImpl.countAll(),
    )
  }
}
