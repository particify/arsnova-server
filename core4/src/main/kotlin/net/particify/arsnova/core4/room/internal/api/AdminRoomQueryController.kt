/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.exception.RoomNotFoundException
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminRoomQueryController(private val roomService: RoomServiceImpl) {

  @QueryMapping
  fun adminRoomByIdOrShortId(@Argument input: AdminRoomQueryInput): Room? {
    if (input.id == null && input.shortId == null) {
      throw InvalidInputException("No id or shortId is provided")
    }
    return if (input.id != null) {
      roomService.findByIdOrNull(input.id)
    } else {
      roomService.findOneByShortId(input.shortId!!)
    } ?: throw RoomNotFoundException()
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun createdBy(room: Room): UUID? {
    return room.auditMetadata.createdBy
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun createdAt(room: Room): OffsetDateTime? {
    return room.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun updatedBy(room: Room): UUID? {
    return room.auditMetadata.updatedBy
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun updatedAt(room: Room): OffsetDateTime? {
    return room.auditMetadata.updatedAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun deletedBy(room: Room): UUID? {
    return room.auditMetadata.deletedBy
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun deletedAt(room: Room): OffsetDateTime? {
    return room.auditMetadata.deletedAt?.atOffset(ZoneOffset.UTC)
  }
}
