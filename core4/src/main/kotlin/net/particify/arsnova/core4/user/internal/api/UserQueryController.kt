/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import net.particify.arsnova.core4.user.User
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Query")
class UserQueryController {
  @QueryMapping
  fun currentUser(@AuthenticationPrincipal user: User): User {
    return user
  }
}
