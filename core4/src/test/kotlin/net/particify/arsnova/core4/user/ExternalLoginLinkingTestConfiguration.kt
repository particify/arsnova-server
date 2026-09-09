/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.user.internal.UserRepository
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/** Selected by the registrations which the linking tests drive their logins through. */
const val RECORDING_LINKING_STRATEGY_NAME = "recording"

/**
 * Registers the test strategy. It is kept apart from the SAML and LDAP test configurations on
 * purpose: the tests which prove that the FOSS behaviour is unchanged have to run with no strategy
 * in the context at all.
 */
@TestConfiguration(proxyBeanMethods = false)
class ExternalLoginLinkingTestConfiguration {
  @Bean
  fun recordingExternalLoginLinkingStrategy(userRepository: UserRepository) =
      RecordingExternalLoginLinkingStrategy(userRepository)
}

/**
 * Records every consultation and returns the account a test has armed it with. The target is held
 * by ID and loaded through the repository, so the returned entity belongs to the session the
 * provisioning code runs in.
 */
class RecordingExternalLoginLinkingStrategy(private val userRepository: UserRepository) :
    ExternalLoginLinkingStrategy {
  override val name = RECORDING_LINKING_STRATEGY_NAME

  data class Consultation(val providerId: UUID, val externalId: String, val mailAddress: String?)

  val consultations = mutableListOf<Consultation>()
  private var linkTargetId: UUID? = null

  override fun findLinkTarget(providerId: UUID, externalId: String, mailAddress: String?): User? {
    consultations += Consultation(providerId, externalId, mailAddress)
    return linkTargetId?.let { userRepository.findByIdOrNull(it) }
  }

  fun armWith(user: User) {
    linkTargetId = checkNotNull(user.id) { "The link target has to be persisted." }
  }

  fun reset() {
    consultations.clear()
    linkTargetId = null
  }
}
