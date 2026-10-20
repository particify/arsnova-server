/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.util.Locale
import net.particify.arsnova.core4.room.RoomUserAlias
import net.particify.arsnova.core4.room.internal.RoomUserAliasServiceImpl
import net.particify.arsnova.core4.user.User
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class RoomUserAliasMutationController(private val roomUserAliasService: RoomUserAliasServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.roomId, 'Room', 'read')")
  fun setRoomUserAlias(
      @Argument input: SetRoomUserAliasInput,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): RoomUserAlias {
    return roomUserAliasService.set(input.roomId, user.id!!, input.alias, input.seed, locale)
  }
}
