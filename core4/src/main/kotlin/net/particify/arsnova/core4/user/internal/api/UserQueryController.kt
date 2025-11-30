/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import java.util.Optional
import net.particify.arsnova.core4.user.QUser
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class UserQueryController(private val userService: UserServiceImpl) {
  @QueryMapping
  fun currentUser(@AuthenticationPrincipal user: User): User {
    return user
  }

  @QueryMapping
  fun userByDisplayId(@Argument displayId: String): User {
    // Using Optional as a workaround. There is a nullability issue with findBy.
    val user: Optional<User> = userService.findBy(QUser.user.username.eq(displayId)) { it.one() }
    return user.orElseThrow { UserNotFoundException() }
  }

  @SchemaMapping(typeName = "User", field = "verified")
  fun verified(user: User): Boolean {
    return user.username != null
  }

  @SchemaMapping(typeName = "User", field = "displayId")
  fun displayId(user: User): String? {
    return user.username ?: user.unverifiedMailAddress
  }
}
