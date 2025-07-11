/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.system.migration.v3.Announcement as AnnouncementV3
import net.particify.arsnova.core4.system.migration.v3.Room as RoomV3
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient

@Component
@ConditionalOnBooleanProperty(name = ["persistence.v3-migration.enabled"])
class Migrator(
    @PersistenceContext private val entityManager: EntityManager,
    private val userService: UserService,
    properties: MigrationProperties
) {
  companion object {
    private const val COUCHDB_RESULT_LIMIT = 200
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val couchdbClient =
      RestClient.builder()
          .baseUrl(properties.couchdbUrl)
          .defaultHeaders {
            it.setBasicAuth("arsnova", "arsnova")
            it.accept = listOf(MediaType.APPLICATION_JSON)
            it.contentType = MediaType.APPLICATION_JSON
          }
          .build()
  private val authzClient =
      RestClient.builder()
          .baseUrl(properties.roomAccessUrl)
          .defaultHeaders {
            it.setBasicAuth("arsnova", "arsnova")
            it.accept = listOf(MediaType.APPLICATION_JSON)
            it.contentType = MediaType.APPLICATION_JSON
          }
          .build()

  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  private inline fun <reified T : Entity> migrate(fn: (doc: T) -> Any?) {
    val design = T::class.simpleName
    var startKey = "\"\""
    do {
      logger.trace(
          "Loading CouchDB data for migration (design: {}, startkey: {})...", design, startKey)
      val result =
          couchdbClient
              .get()
              .uri {
                it.path("/_design/$design/_view/by_id")
                    .queryParam("reduce", false)
                    .queryParam("include_docs", true)
                    .queryParam("startkey", startKey)
                    .queryParam("limit", COUCHDB_RESULT_LIMIT + 1)
                    .build()
              }
              .retrieve()
              .body(typeReference<CouchdbResponse<T>>())!!
      result.rows.take(COUCHDB_RESULT_LIMIT).forEach {
        logger.debug("Migrating {} {}...", design, it.doc.id)
        val newEntity = fn(it.doc)
        if (newEntity != null) {
          entityManager.persist(newEntity)
          entityManager.flush()
        }
      }
      entityManager.flush()
      if (result.rows.size > COUCHDB_RESULT_LIMIT)
          startKey = "\"" + result.rows[COUCHDB_RESULT_LIMIT].doc.id + "\""
    } while (result.rows.size > COUCHDB_RESULT_LIMIT)
  }

  @Transactional
  fun migrateUsers() {
    logger.info("Migrating UserProfile data...")
    val defaultRole = userService.findRoleByName("USER")
    migrate<UserProfile> {
      val settings = mutableMapOf<String, Any>()
      if (it.settings?.contentAnswersDirectlyBelowChart == true)
          settings["contentAnswersDirectlyBelowChart"] = true
      if (it.settings?.contentVisualizationUnitPercent == true)
          settings["contentVisualizationUnitPercent"] = true
      if (it.settings?.rotateWordcloudItems == false) settings["rotateWordcloudItems"] = false
      if (it.settings?.showContentResultsDirectly == true)
          settings["showContentResultsDirectly"] = true
      User(
          id = UuidHelper.stringToUuid(it.id),
          auditMetadata =
              AuditMetadata(createdAt = it.creationTimestamp, updatedAt = it.updateTimestamp),
          username = it.loginId,
          password = if (it.account.password != null) "{bcrypt}" + it.account.password else null,
          mailAddress = it.person?.mail,
          givenName = it.person?.firstName,
          surname = it.person?.lastName,
          settings = settings,
          announcementsReadAt = it.announcementReadTimestamp,
          roles = mutableListOf(defaultRole))
    }
  }

  @Transactional
  fun migrateRooms() {
    logger.info("Migrating Room data...")
    migrate<RoomV3> {
      val settings = mutableMapOf<String, Any>()
      val userId = UuidHelper.stringToUuid(it.ownerId)
      val user = entityManager.find(User::class.java, userId)
      if (user == null) {
        logger.warn("Cannot create Room. User not found: {}", userId)
        return@migrate null
      }
      val newRoom =
          Room(
              id = UuidHelper.stringToUuid(it.id),
              auditMetadata =
                  AuditMetadata(
                      createdAt = it.creationTimestamp,
                      updatedAt = it.updateTimestamp,
                      createdBy = user.id),
              shortId = it.shortId.toInt(),
              name = it.name,
              description = it.description,
              settings = settings)
      migrateMemberships(newRoom)
      newRoom
    }
  }

  @Transactional
  fun migrateMemberships(room: Room) {
    logger.debug("Migrating RoomAccess for Room {}...", room.id)
    val roomAccessList =
        authzClient
            .get()
            .uri("/by-room/${room.id}")
            .retrieve()
            .body(typeReference<List<RoomAccess>>())!!
    roomAccessList.forEach {
      logger.debug("Migrating RoomAccess ({}, {})...", it.roomId, it.userId)
      val userId = UuidHelper.stringToUuid(it.userId)
      val user = entityManager.find(User::class.java, userId)
      if (user == null) {
        logger.warn("Cannot create Membership. User not found: {}", userId)
        return@forEach
      }
      val newMembership =
          Membership(
              room = room,
              user = user,
              role = RoomRole.valueOf(it.role.name),
              lastActivityAt = it.lastAccess)
      entityManager.persist(newMembership)
    }
  }

  @Transactional
  fun migrateAnnouncements() {
    logger.info("Migrating Announcement data...")
    migrate<AnnouncementV3> {
      val roomId = UuidHelper.stringToUuid(it.roomId)
      val room = entityManager.find(Room::class.java, roomId)
      if (room == null) {
        logger.warn("Cannot create Announcement. Room not found: {}", roomId)
        return@migrate null
      }
      Announcement(
          id = UuidHelper.stringToUuid(it.id),
          auditMetadata =
              AuditMetadata(
                  createdAt = it.creationTimestamp,
                  updatedAt = it.updateTimestamp,
                  createdBy = UuidHelper.stringToUuid(it.creatorId)),
          room = room,
          title = it.title,
          body = it.body)
    }
  }
}
