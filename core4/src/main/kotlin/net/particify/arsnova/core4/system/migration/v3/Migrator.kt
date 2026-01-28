/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.RoomRepository
import net.particify.arsnova.core4.system.migration.v3.Announcement as AnnouncementV3
import net.particify.arsnova.core4.system.migration.v3.Room as RoomV3
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.ExternalLogin
import net.particify.arsnova.core4.user.internal.RoleRepository
import net.particify.arsnova.core4.user.internal.UserRepository
import org.hibernate.Session
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
@ConditionalOnBooleanProperty(name = ["persistence.v3-migration.enabled"])
class Migrator(
    @PersistenceContext private val entityManager: EntityManager,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val roomRepository: RoomRepository,
    private val properties: MigrationProperties,
) {
  companion object {
    private const val JDBC_BATCH_SIZE = 50
    private const val COUCHDB_RESULT_LIMIT = 200
    private const val COUCHDB_PROGRESS_INTERVAL = COUCHDB_RESULT_LIMIT * 5
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val couchdbClient =
      RestClient.builder()
          .baseUrl(properties.couchdb.url)
          .defaultHeaders {
            it.setBasicAuth(properties.couchdb.username, properties.couchdb.password)
            it.accept = listOf(MediaType.APPLICATION_JSON)
            it.contentType = MediaType.APPLICATION_JSON
          }
          .build()
  private val authzClient =
      RestClient.builder()
          .baseUrl(properties.roomAccessUrl)
          .defaultHeaders {
            it.accept = listOf(MediaType.APPLICATION_JSON)
            it.contentType = MediaType.APPLICATION_JSON
          }
          .build()

  private inline fun <reified T : Any> typeReference() = object : ParameterizedTypeReference<T>() {}

  private inline fun <reified T : Entity> migrate(fn: (doc: T) -> Any?) {
    val design = T::class.simpleName
    var startKey = "\"\""
    var count = 0
    val migrationStart = Instant.now()
    entityManager.unwrap<Session>(Session::class.java).jdbcBatchSize = JDBC_BATCH_SIZE
    do {
      logger.trace(
          "Loading CouchDB data for migration (design: {}, startkey: {})...", design, startKey)
      val queryStart = Instant.now()
      val result =
          try {
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
          } catch (e: RestClientException) {
            logger.error("Failed to parse {} document (startkey: {})", design, startKey)
            throw e
          }
      logger.debug("Query took {} ms.", queryStart.until(Instant.now(), ChronoUnit.MILLIS))
      result.rows.take(COUCHDB_RESULT_LIMIT).chunked(JDBC_BATCH_SIZE).forEach { chunk ->
        chunk.forEach {
          logger.trace("Migrating {} {}...", design, it.doc.id)
          val newEntity = fn(it.doc)
          if (newEntity != null) {
            entityManager.persist(newEntity)
          }
        }
        val flushStart = Instant.now()
        entityManager.flush()
        entityManager.clear()
        logger.debug("Flush took {} ms.", flushStart.until(Instant.now(), ChronoUnit.MILLIS))
      }
      count += result.rows.size.coerceAtMost(COUCHDB_RESULT_LIMIT)
      if (count % COUCHDB_PROGRESS_INTERVAL == 0) {
        logger.info("Migration progress: {} {} documents migrated.", count, design)
      }
      if (result.rows.size > COUCHDB_RESULT_LIMIT)
          startKey = "\"" + result.rows[COUCHDB_RESULT_LIMIT].doc.id + "\""
    } while (result.rows.size > COUCHDB_RESULT_LIMIT)
    logger.info(
        "Migration of {} {} documents completed in {} seconds.",
        count,
        design,
        migrationStart.until(Instant.now(), ChronoUnit.SECONDS))
  }

  @Transactional
  fun migrateUsers() {
    logger.info("Migrating UserProfile data...")
    val defaultRole = roleRepository.findByName("USER")
    val defaultRoleRef = roleRepository.getReferenceById(defaultRole.id!!)
    migrate<UserProfile> {
      val settings = mutableMapOf<String, Any>()
      if (it.settings?.contentAnswersDirectlyBelowChart == true)
          settings["contentAnswersDirectlyBelowChart"] = true
      if (it.settings?.contentVisualizationUnitPercent == true)
          settings["contentVisualizationUnitPercent"] = true
      if (it.settings?.rotateWordcloudItems == false) settings["rotateWordcloudItems"] = false
      if (it.settings?.showContentResultsDirectly == true)
          settings["showContentResultsDirectly"] = true
      val newUser =
          User(
              id = UuidHelper.stringToUuid(it.id),
              auditMetadata =
                  AuditMetadata(createdAt = it.creationTimestamp, updatedAt = it.updateTimestamp),
              username = it.loginId,
              password =
                  if (it.account?.password != null) "{bcrypt}" + it.account.password else null,
              mailAddress = it.person?.mail,
              givenName = it.person?.firstName,
              surname = it.person?.lastName,
              uiSettings = settings,
              announcementsReadAt = it.announcementReadTimestamp,
              roles = mutableListOf(defaultRoleRef))
      if (!migrateExternalLogins(newUser, it)) {
        return@migrate null
      }
      newUser
    }
  }

  private fun migrateExternalLogins(newUser: User, userProfile: UserProfile): Boolean {
    var error = false
    when (userProfile.authProvider) {
      UserProfile.AuthProvider.ANONYMIZED -> {
        newUser.clearForSoftDelete()
        newUser.auditMetadata.deletedAt = Instant.now()
      }
      UserProfile.AuthProvider.ARSNOVA_GUEST -> {
        newUser.username = null
      }
      UserProfile.AuthProvider.ARSNOVA -> {}
      UserProfile.AuthProvider.CAS,
      UserProfile.AuthProvider.LDAP,
      UserProfile.AuthProvider.OIDC,
      UserProfile.AuthProvider.SAML -> {
        val providerId = properties.authenticationProviderMapping[userProfile.authProvider.name]
        if (providerId == null) {
          logger.warn(
              "No ID mapping for authentication provider found: {}", userProfile.authProvider)
          error = true
        }
        val externalLogin =
            ExternalLogin(
                user = newUser,
                providerId = providerId,
                externalId = userProfile.loginId,
                auditMetadata = AuditMetadata(createdAt = Instant.now()))
        newUser.externalLogins += externalLogin
        newUser.username = null
      }
      else -> {
        logger.warn("Unsupported authentication provider: {}", userProfile.authProvider)
        error = true
      }
    }
    return !error
  }

  @Transactional
  fun migrateRooms() {
    logger.info("Migrating Room data...")
    migrate<RoomV3> {
      val userId = UuidHelper.stringToUuid(it.ownerId)!!
      if (!userRepository.existsById(userId)) {
        logger.warn("Cannot create Room. User not found: {}", userId)
        return@migrate null
      }
      val userRef = userRepository.getReferenceById(userId)
      val newRoom =
          Room(
              id = UuidHelper.stringToUuid(it.id),
              auditMetadata =
                  AuditMetadata(
                      createdAt = it.creationTimestamp,
                      updatedAt = it.updateTimestamp,
                      createdBy = userRef.id),
              shortId = it.shortId.toInt(),
              name = it.name,
              description = it.description)
      migrateMemberships(newRoom)
      newRoom
    }
  }

  @Transactional
  fun migrateMemberships(room: Room) {
    logger.trace("Migrating RoomAccess for Room {}...", room.id)
    val roomAccessList =
        authzClient
            .get()
            .uri("/by-room/${room.id}")
            .retrieve()
            .body(typeReference<List<RoomAccess>>())!!
    roomAccessList.forEach {
      logger.trace("Migrating RoomAccess ({}, {})...", it.roomId, it.userId)
      val userId = UuidHelper.stringToUuid(it.userId)!!
      val userRef = userRepository.getReferenceById(userId)
      val newMembership =
          Membership(
              room = room,
              user = userRef,
              role = RoomRole.valueOf(it.role.name),
              lastActivityAt = it.lastAccess)
      room.userRoles.add(newMembership)
    }
  }

  @Transactional
  fun migrateAnnouncements() {
    logger.info("Migrating Announcement data...")
    migrate<AnnouncementV3> {
      val roomId = UuidHelper.stringToUuid(it.roomId)!!
      if (!roomRepository.existsById(roomId)) {
        logger.warn(
            "Room {} for announcement {} not found. Skipping announcement creation.", roomId, it.id)
        return@migrate null
      }
      val roomRef = roomRepository.getReferenceById(roomId)
      Announcement(
          id = UuidHelper.stringToUuid(it.id),
          auditMetadata =
              AuditMetadata(
                  createdAt = it.creationTimestamp,
                  updatedAt = it.updateTimestamp,
                  createdBy = UuidHelper.stringToUuid(it.creatorId)),
          room = roomRef,
          title = it.title,
          body = it.body)
    }
  }
}
