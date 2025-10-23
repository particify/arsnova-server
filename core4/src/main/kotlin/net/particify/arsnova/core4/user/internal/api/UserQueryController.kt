/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.UserRepository
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Query")
class UserQueryController(private val userRepository: UserRepository) {
  @QueryMapping
  fun currentUser(@AuthenticationPrincipal user: User): User {
    return user
  }

  @QueryMapping
  fun users(@Argument user: User, subrange: ScrollSubrange): Window<User> {
    val matcher = ExampleMatcher.matchingAll().withIgnorePaths("version")
    return userRepository.findBy(Example.of(user, matcher)) { q ->
      q.scroll(subrange.position().orElse(ScrollPosition.keyset()))
    }
  }
}
