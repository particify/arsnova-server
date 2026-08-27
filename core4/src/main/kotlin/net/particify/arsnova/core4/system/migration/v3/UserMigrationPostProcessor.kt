/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.UsernameMapping
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Promotes a parked mail address to the account ranked highest for it and leaves it parked on every
 * other account. An address which some account already holds is skipped, which is what makes a
 * local account win against an external one without a rule of its own: the migration writes a local
 * account's address directly.
 *
 * `DISTINCT ON` reduces the candidates to one row per address, so the statement cannot assign the
 * same value twice, and the full ordering makes the winner reproducible across runs.
 */
private val SETTLE_MAIL_ADDRESSES =
    """
    UPDATE "user"."user" AS u
    SET mail_address = u.unverified_mail_address, unverified_mail_address = NULL
    FROM (
      SELECT DISTINCT ON (parked.unverified_mail_address)
        parked.id, parked.unverified_mail_address, parked.last_activity_at, parked.created_at
      FROM "user"."user" AS parked
      WHERE parked.deleted = false
        AND parked.unverified_mail_address IS NOT NULL
        AND NOT EXISTS (
          SELECT 1 FROM "user"."user" AS holder
          WHERE holder.mail_address = parked.unverified_mail_address)
      ORDER BY parked.unverified_mail_address, parked.last_activity_at DESC NULLS LAST,
        parked.created_at ASC, parked.id ASC
    ) AS winner
    WHERE u.id = winner.id
    """
        .trimIndent()

/**
 * Assigns the settled mail address as the username, for the one provider which maps it that way.
 * Runs before [SETTLE_USERNAMES_FROM_EXTERNAL_ID], which then picks up whatever this leaves unset.
 */
private val SETTLE_USERNAMES_FROM_MAIL_ADDRESS =
    """
    UPDATE "user"."user" AS u
    SET username = winner.candidate
    FROM (
      SELECT DISTINCT ON (ranked.candidate)
        ranked.id, ranked.candidate, ranked.last_activity_at, ranked.created_at
      FROM (
        SELECT usr.id, lower(usr.mail_address) AS candidate, usr.last_activity_at, usr.created_at
        FROM "user"."user" AS usr
        JOIN "user"."external_login" AS el ON el.user_id = usr.id
        WHERE usr.deleted = false
          AND usr.username IS NULL
          AND el.provider_id = cast(:providerId AS uuid)
      ) AS ranked
      WHERE ranked.candidate IS NOT NULL
        AND NOT EXISTS (
          SELECT 1 FROM "user"."user" AS holder WHERE holder.username = ranked.candidate)
      ORDER BY ranked.candidate, ranked.last_activity_at DESC NULLS LAST,
        ranked.created_at ASC, ranked.id ASC
    ) AS winner
    WHERE u.id = winner.id
    """
        .trimIndent()

/**
 * Assigns the external ID as the username wherever one is still unset. The inner `DISTINCT ON`
 * keeps an account holding logins for several providers from being given two different usernames by
 * a single statement.
 */
private val SETTLE_USERNAMES_FROM_EXTERNAL_ID =
    """
    UPDATE "user"."user" AS u
    SET username = winner.candidate
    FROM (
      SELECT DISTINCT ON (ranked.candidate)
        ranked.id, ranked.candidate, ranked.last_activity_at, ranked.created_at
      FROM (
        SELECT DISTINCT ON (usr.id)
          usr.id, el.provider_id, lower(el.external_id) AS candidate, usr.last_activity_at,
          usr.created_at
        FROM "user"."user" AS usr
        JOIN "user"."external_login" AS el ON el.user_id = usr.id
        WHERE usr.deleted = false AND usr.username IS NULL
        ORDER BY usr.id, el.provider_id
      ) AS ranked
      WHERE ranked.candidate IS NOT NULL
        AND NOT EXISTS (
          SELECT 1 FROM "user"."user" AS holder WHERE holder.username = ranked.candidate)
      ORDER BY ranked.candidate, ranked.last_activity_at DESC NULLS LAST,
        ranked.created_at ASC, ranked.id ASC
    ) AS winner
    WHERE u.id = winner.id
    """
        .trimIndent()

private val PARKED_MAIL_ADDRESSES =
    """
    SELECT u.unverified_mail_address
    FROM "user"."user" AS u
    WHERE u.deleted = false AND u.unverified_mail_address IS NOT NULL
    ORDER BY u.unverified_mail_address
    """
        .trimIndent()

/**
 * Settles the mail addresses and usernames which [Migrator] deliberately leaves unset for external
 * accounts, once every user exists.
 *
 * Both columns are unique, and the account which keeps a value held by several of them cannot be
 * picked while documents are streamed in: the insert flushes and clears per chunk, so a query sees
 * neither pending rows nor the rows an earlier chunk contributed. Bulk statements over the finished
 * table avoid that without holding per-row state, and each of them only touches rows which are
 * still unset, so running them again is a no-op rather than a second guess.
 */
@Component
class UserMigrationPostProcessor(
    @PersistenceContext private val entityManager: EntityManager,
    private val properties: MigrationProperties,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun settleIdentities() {
    // Bulk statements do not see the persistence context, so pending inserts are written first.
    entityManager.flush()
    val settled = entityManager.createNativeQuery(SETTLE_MAIL_ADDRESSES).executeUpdate()
    logger.info("Settled the mail address of {} migrated accounts.", settled)
    settleUsernames()
    reportParkedMailAddresses()
  }

  /**
   * Assigns the preferred candidate where it is free and falls back to the external ID. An account
   * for which neither is free keeps no username and stays unverified, the same outcome the login
   * paths produce for a collision.
   */
  private fun settleUsernames() {
    var settled = 0
    val mailMappedProviderId = mailMappedProviderId()
    if (mailMappedProviderId != null) {
      settled +=
          entityManager
              .createNativeQuery(SETTLE_USERNAMES_FROM_MAIL_ADDRESS)
              .setParameter("providerId", mailMappedProviderId.toString())
              .executeUpdate()
    }
    settled += entityManager.createNativeQuery(SETTLE_USERNAMES_FROM_EXTERNAL_ID).executeUpdate()
    logger.info("Settled the username of {} migrated accounts.", settled)
  }

  /**
   * The provider whose username is the mail address rather than the external ID. Only SAML offers
   * that mapping, and the migration configuration names at most one provider ID per v3 provider.
   */
  private fun mailMappedProviderId(): UUID? {
    val providerId =
        properties.authenticationProviderMapping[UserProfile.AuthProvider.SAML.name] ?: return null
    val registration = saml2Properties.registration[providerId]
    return if (registration?.usernameMapping == UsernameMapping.MAIL_ADDRESS) providerId else null
  }

  @Suppress("UNCHECKED_CAST")
  private fun reportParkedMailAddresses() {
    val query = entityManager.createNativeQuery(PARKED_MAIL_ADDRESSES)
    val parked = query.resultList as List<String>
    if (parked.isEmpty()) {
      return
    }
    val addresses = parked.distinct()
    logger.warn(
        "{} accounts kept their mail address unverified because another one already holds it. " +
            "{} addresses are affected; they are the ones left in unverified_mail_address.",
        parked.size,
        addresses.size)
    addresses.forEach { logger.debug("Mail address held by more than one account: {}", it) }
  }
}
