/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.room.RoomUserAlias
import net.particify.arsnova.core4.room.internal.RoomUserAliasServiceImpl
import net.particify.arsnova.core4.user.User
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class RoomUserAliasQueryController(private val roomUserAliasService: RoomUserAliasServiceImpl) {
  /** Returns an unpersisted proposal without an ID if the user has not set an alias yet. */
  @QueryMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  fun roomUserAliasByRoomId(
      @Argument roomId: UUID,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): RoomUserAlias {
    return roomUserAliasService.retrieveOrGenerate(roomId, user.id!!, locale)
  }
}
