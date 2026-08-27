/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.time.Instant
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.announcement.internal.AnnouncementRepository
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.RoomRepository
import net.particify.arsnova.core4.system.migration.v3.Announcement as AnnouncementV3
import net.particify.arsnova.core4.system.migration.v3.MigrationHelper.typeReference
import net.particify.arsnova.core4.system.migration.v3.Room as RoomV3
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExternalLogin
import net.particify.arsnova.core4.user.internal.RoleRepository
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient

const val JDBC_BATCH_SIZE = 50

/** Handles migration of data for users, rooms and announcements from v3. */
@Component
@ConditionalOnBooleanProperty(name = ["persistence.v3-migration.enabled"])
@Suppress("LongParameterList")
class Migrator(
    @PersistenceContext private val entityManager: EntityManager,
    private val couchdbMigrator: CouchdbMigrator,
    private val userService: UserServiceImpl,
    private val roleRepository: RoleRepository,
    private val roomRepository: RoomRepository,
    private val announcementRepository: AnnouncementRepository,
    private val postProcessor: UserMigrationPostProcessor,
    private val properties: MigrationProperties,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties,
) {
  companion object {
    private const val MAX_ROOM_NAME_LENGTH = 50
    private const val MAX_ROOM_DESCRIPTION_LENGTH = 2000
    private const val MAX_ANNOUNCEMENT_TITLE_LENGTH = 100
    private const val MAX_ANNOUNCEMENT_BODY_LENGTH = 2000
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val authzClient =
      RestClient.builder()
          .baseUrl(properties.roomAccessUrl)
          .defaultHeaders {
            it.accept = listOf(MediaType.APPLICATION_JSON)
            it.contentType = MediaType.APPLICATION_JSON
          }
          .build()

  @Transactional
  fun migrateUsers() {
    logger.info("Migrating UserProfile data...")
    val count = userService.count()
    if (count > 0) {
      logger.warn("User table not empty ({}), skipping migration.", count)
      return
    }
    val defaultRole = roleRepository.findByName("USER")
    val defaultRoleRef = roleRepository.getReferenceById(defaultRole.id!!)
    couchdbMigrator.migrate<UserProfile> {
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
              lastActivityAt = it.lastActivityTimestamp,
              announcementsReadAt = it.announcementReadTimestamp,
              roles = mutableSetOf(defaultRoleRef))
      if (!migrateExternalLogins(newUser, it)) {
        return@migrate null
      }
      newUser
    }
    postProcessor.settleIdentities()
  }

  private fun migrateExternalLogins(newUser: User, userProfile: UserProfile): Boolean {
    return when (userProfile.authProvider) {
      UserProfile.AuthProvider.ANONYMIZED -> {
        newUser.clearForSoftDelete()
        newUser.deletedAt = Instant.now()
        true
      }
      UserProfile.AuthProvider.ARSNOVA_GUEST -> {
        // A guest account carries no mail address at all, not even an unverified one: that would
        // still surface as the account's display ID.
        newUser.mailAddress = null
        newUser.unverifiedMailAddress = null
        newUser.username = null
        true
      }
      UserProfile.AuthProvider.ARSNOVA -> {
        newUser.mailAddress = userProfile.loginId
        true
      }
      UserProfile.AuthProvider.CAS,
      UserProfile.AuthProvider.LDAP,
      UserProfile.AuthProvider.OIDC,
      UserProfile.AuthProvider.SAML -> migrateExternalLogin(newUser, userProfile)
      else -> {
        logger.warn("Unsupported authentication provider: {}", userProfile.authProvider)
        false
      }
    }
  }

  /**
   * Leaves the unique columns empty and parks the mail address instead of writing it: the account
   * which keeps an address shared by several users cannot be determined while documents are
   * streamed in, so [UserMigrationPostProcessor] settles both columns once all users exist.
   */
  private fun migrateExternalLogin(newUser: User, userProfile: UserProfile): Boolean {
    val authProvider = userProfile.authProvider
    val providerId = properties.authenticationProviderMapping[authProvider.name]
    if (providerId == null) {
      logger.warn("No ID mapping for authentication provider found: {}", authProvider)
      return false
    }
    if (authProvider == UserProfile.AuthProvider.SAML &&
        saml2Properties.registration[providerId] == null) {
      logger.warn(
          "No SAML registration found for provider {}. Without it the username mapping is " +
              "unknown, and a wrong username would be permanent.",
          providerId)
      return false
    }
    newUser.unverifiedMailAddress = newUser.mailAddress
    newUser.mailAddress = null
    newUser.username = null
    newUser.externalLogins +=
        ExternalLogin(
            user = newUser,
            providerId = providerId,
            externalId = externalId(authProvider, userProfile.loginId),
            auditMetadata = AuditMetadata(createdAt = Instant.now()))
    return true
  }

  /**
   * v3 lowercased login IDs for local accounts only, while core4 canonicalizes LDAP user IDs to
   * lowercase. A mixed-case value would not be found on the first login, and a second account would
   * be created for the same user.
   */
  private fun externalId(authProvider: UserProfile.AuthProvider, loginId: String): String =
      if (authProvider == UserProfile.AuthProvider.LDAP) loginId.lowercase() else loginId

  @Transactional
  fun migrateRooms() {
    logger.info("Migrating Room data...")
    val count = roomRepository.count()
    if (count > 0) {
      logger.warn("Room table not empty ({}), skipping migration.", count)
      return
    }
    val ghostUser = userService.getOrCreateGhostUser()
    couchdbMigrator.migrate<RoomV3> {
      val roomId = UuidHelper.stringToUuid(it.id)
      var userId = UuidHelper.stringToUuid(it.ownerId)!!
      if (!userService.existsById(userId)) {
        // Room might have been transferred to another user.
        // Despite the property name "ownerId", v3 did not update it.
        logger.info("Creator {} for room {} not found. Using ghost user.", userId, roomId)
        userId = ghostUser.id!!
      }
      val userRef = userService.getReferenceById(userId)
      // Bug: creationTimestamp might not have been set or incorrectly set to EPOCH.
      // In those cases, updateTimestamp has been set and can be used as fallback.
      var createdAt =
          if (it.creationTimestamp != null && it.creationTimestamp > Instant.EPOCH)
              it.creationTimestamp
          else
              it.updateTimestamp
                  ?: error("No suitable value for createdAt available for room $roomId.")
      val name =
          if (it.name.length > MAX_ROOM_NAME_LENGTH) {
            logger.warn("Truncating name for room {}.", roomId)
            it.name.substring(0, MAX_ROOM_NAME_LENGTH - 1)
          } else it.name
      val description =
          if (it.description.length > MAX_ROOM_DESCRIPTION_LENGTH) {
            logger.warn("Truncating description for room {}.", roomId)
            it.description.substring(0, MAX_ROOM_DESCRIPTION_LENGTH - 1)
          } else it.description
      val metadata = mutableMapOf<String, Any>()
      if (it.importMetadata != null) {
        when (it.importMetadata.source) {
          "DUPLICATION" -> metadata["duplicated"] = true
          "V2_IMPORT" -> {
            // The original timestamp was used by v3 as creationTimestamp.
            // Using the creation in the system makes more sense, so we swap them.
            metadata["originallyCreatedAt"] = createdAt
            metadata["importSource"] = "v2"
            createdAt = it.importMetadata.timestamp
          }
        }
      }
      val newRoom =
          Room(
              id = roomId,
              auditMetadata =
                  AuditMetadata(
                      createdAt = createdAt,
                      updatedAt = it.updateTimestamp,
                      createdBy = userRef.id),
              shortId = it.shortId.toInt(),
              name = name,
              description = description,
              metadata = metadata)

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
    // Bug: v3 did not delete mapping when a user was deleted.
    // Retrieve user IDs so the creation of memberships can be skipped for non-existing users.
    val userIds = roomAccessList.mapNotNull { UuidHelper.stringToUuid(it.userId) }
    val existingUserIds = userService.findAllById(userIds).map { it.id }
    roomAccessList.forEach {
      logger.trace("Migrating RoomAccess ({}, {})...", it.roomId, it.userId)
      val userId = UuidHelper.stringToUuid(it.userId)!!
      if (!existingUserIds.contains(userId)) {
        val logMessage = "User {} does not exist. Skipping membership ({}) creation for room {}."
        if (it.role.name == "OWNER") {
          logger.warn(logMessage, userId, it.role.name, room.id)
        } else {
          logger.debug(logMessage, userId, it.role.name, room.id)
        }
        return@forEach
      }
      val userRef = userService.getReferenceById(userId)
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
    val count = announcementRepository.count()
    if (count > 0) {
      logger.warn("Announcement table not empty ({}), skipping migration.", count)
      return
    }
    val ghostUser = userService.getOrCreateGhostUser()
    couchdbMigrator.migrate<AnnouncementV3> {
      val announcementId = UuidHelper.stringToUuid(it.id)
      val roomId = UuidHelper.stringToUuid(it.roomId)!!
      if (!roomRepository.existsById(roomId)) {
        logger.warn(
            "Room {} for announcement {} not found. Skipping announcement creation.",
            roomId,
            announcementId)
        return@migrate null
      }
      val roomRef = roomRepository.getReferenceById(roomId)
      var userId = UuidHelper.stringToUuid(it.creatorId)!!
      if (!userService.existsById(userId)) {
        logger.info(
            "Creator {} for announcement {} not found. Using ghost user.", userId, announcementId)
        userId = ghostUser.id!!
      }
      val title =
          if (it.title.length > MAX_ANNOUNCEMENT_TITLE_LENGTH) {
            logger.warn("Truncating title for announcement {}.", announcementId)
            it.title.substring(0, MAX_ANNOUNCEMENT_TITLE_LENGTH - 1)
          } else it.title
      val body =
          if (it.body.length > MAX_ANNOUNCEMENT_BODY_LENGTH) {
            logger.warn("Truncating body for announcement {}.", announcementId)
            it.body.substring(0, MAX_ANNOUNCEMENT_BODY_LENGTH - 1)
          } else it.body
      Announcement(
          id = announcementId,
          auditMetadata =
              AuditMetadata(
                  createdAt = it.creationTimestamp,
                  updatedAt = it.updateTimestamp,
                  createdBy = userId),
          room = roomRef,
          title = title,
          body = body)
    }
  }
}
