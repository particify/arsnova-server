/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class UserProfile(
    @field:JsonProperty("_id") override val id: String,
    override val creationTimestamp: Instant,
    override val updateTimestamp: Instant?,
    val lastActivityTimestamp: Instant,
    val authProvider: AuthProvider,
    val loginId: String,
    val account: Account?,
    val person: Person?,
    val settings: Settings?,
    val announcementReadTimestamp: Instant?
) : Entity {
  enum class AuthProvider {
    NONE,
    UNKNOWN,
    ANONYMIZED,
    ARSNOVA,
    ARSNOVA_GUEST,
    LDAP,
    SAML,
    OIDC,
    CAS
  }

  data class Account(
      val password: String?,
      val activationKey: String?,
      val passwordResetKey: String?,
      val passwordResetTime: Instant?
  )

  data class Person(
      val mail: String?,
      val firstName: String?,
      val lastName: String?,
      val displayName: String?
  )

  data class Settings(
      val contentAnswersDirectlyBelowChart: Boolean = false,
      val contentVisualizationUnitPercent: Boolean = false,
      val showContentResultsDirectly: Boolean = false,
      val rotateWordcloudItems: Boolean = true
  )
}
