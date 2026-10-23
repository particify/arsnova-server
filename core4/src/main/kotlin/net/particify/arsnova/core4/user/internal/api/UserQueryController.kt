/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import java.util.Locale
import java.util.Optional
import net.particify.arsnova.core4.user.QUser
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import net.particify.arsnova.core4.user.internal.PasswordPolicy
import net.particify.arsnova.core4.user.internal.PasswordStrength
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
class UserQueryController(
    private val userService: UserServiceImpl,
    private val passwordPolicy: PasswordPolicy
) {
  @QueryMapping
  fun currentUser(@AuthenticationPrincipal user: User): User {
    return user
  }

  @QueryMapping
  @PreAuthorize("hasRole('VERIFIED')")
  fun userByDisplayId(@Argument displayId: String): User {
    // Using Optional as a workaround. There is a nullability issue with findBy.
    val user: Optional<User> =
        userService.findBy(QUser.user.username.eq(displayId.lowercase())) { it.one() }
    return user.orElseThrow { UserNotFoundException() }
  }

  /**
   * The caller's own account data, if there is an account, is added to the inputs. The passed mail
   * address is never used to look up an account, since the result would then disclose that
   * account's data.
   */
  @QueryMapping
  @PreAuthorize("hasRole('USER') or hasRole('CHALLENGE_SOLVED_REUSABLE')")
  fun userPasswordStrength(
      @Argument query: UserPasswordStrengthQueryInput,
      @AuthenticationPrincipal user: User?,
      locale: Locale
  ): PasswordStrength {
    val inputs =
        listOf(
            user?.mailAddress,
            user?.unverifiedMailAddress,
            user?.givenName,
            user?.surname,
            query.mailAddress)
    return passwordPolicy.estimateStrength(query.password, inputs, locale)
  }

  @SchemaMapping(typeName = "User", field = "verified")
  @PreAuthorize("authenticated")
  fun verified(user: User): Boolean {
    return user.username != null
  }

  @SchemaMapping(typeName = "User", field = "displayId")
  @PreAuthorize("authenticated")
  fun displayId(user: User): String? {
    return user.username ?: user.unverifiedMailAddress
  }

  /** How another account signs in is not disclosed, so the field stays unresolved for it. */
  @SchemaMapping(typeName = "User", field = "localPasswordSet")
  @PreAuthorize("authenticated")
  fun localPasswordSet(user: User, @AuthenticationPrincipal principal: User?): Boolean? {
    if (principal?.id != user.id) {
      return null
    }
    return user.password != null
  }
}
