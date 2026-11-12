/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.net.http.HttpClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.sso.OidcProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ClientRegistrations
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private val logger = LoggerFactory.getLogger(OidcClientRegistrationRepository::class.java)

private const val DISCOVERY_PATH = "/.well-known/openid-configuration"
private const val CONNECT_TIMEOUT_SECONDS = 5L
private const val READ_TIMEOUT_SECONDS = 10L
private val DEFAULT_RETRY_DELAY = Duration.ofSeconds(30)

/**
 * Builds a [ClientRegistration] from the provider's discovery document on first use rather than
 * while the application starts, so a provider which is down does not keep the application from
 * starting and recovers without a restart. A document which was read is kept; one which could not
 * be read, or which describes no usable provider, is tried again on a lookup at least [retryDelay]
 * later, which keeps a provider that is down from being asked on every attempt to log in. Only such
 * failures of the provider are retried: [OidcProperties] rejects a malformed configuration while
 * the application starts.
 *
 * Deliberately not a bean: other code looks a `ClientRegistrationRepository` up by type, and must
 * not find these registrations.
 */
class OidcClientRegistrationRepository(
    oidcProperties: OidcProperties,
    private val retryDelay: Duration = DEFAULT_RETRY_DELAY,
    private val clock: Clock = Clock.systemUTC()
) : ClientRegistrationRepository {
  private val restClient = discoveryRestClient()
  private val registrations: Map<String, LazyRegistration> =
      oidcProperties.registration.entries.associate { (id, registration) ->
        id.toString() to LazyRegistration(id, registration)
      }

  override fun findByRegistrationId(registrationId: String): ClientRegistration? =
      registrations[registrationId]?.let { resolve(it) }

  private fun resolve(lazy: LazyRegistration): ClientRegistration? =
      lazy.resolved
          ?: synchronized(lazy) {
            lazy.resolved ?: if (isAttemptDue(lazy)) discover(lazy) else null
          }

  private fun isAttemptDue(lazy: LazyRegistration): Boolean =
      lazy.failedAt?.let { !clock.instant().isBefore(it.plus(retryDelay)) } ?: true

  private fun discover(lazy: LazyRegistration): ClientRegistration? =
      try {
        val resolved = build(lazy.id, lazy.registration)
        if (lazy.failedAt != null) {
          logger.info("OIDC registration {}: the provider's discovery document was read.", lazy.id)
        }
        lazy.resolved = resolved
        resolved
      } catch (e: RestClientException) {
        failed(lazy, e)
      } catch (e: IllegalStateException) {
        failed(lazy, e)
      } catch (e: IllegalArgumentException) {
        failed(lazy, e)
      }

  private fun failed(lazy: LazyRegistration, e: RuntimeException): ClientRegistration? {
    if (lazy.failedAt == null) {
      logger.error(
          "OIDC registration {}: the discovery document of {} could not be read or used, so " +
              "the registration authenticates nobody until it can: {}",
          lazy.id,
          lazy.registration.issuerUri,
          e.toString())
    }
    lazy.failedAt = clock.instant()
    return null
  }

  /**
   * The issuer is compared as OpenID Connect Discovery requires: a document naming any other issuer
   * could hand out endpoints of a provider which was never configured.
   */
  private fun build(id: UUID, registration: OidcProperties.Registration): ClientRegistration {
    val issuer = registration.issuerUri
    val document =
        checkNotNull(
            restClient
                .get()
                .uri(issuer.removeSuffix("/") + DISCOVERY_PATH)
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})) {
              "The discovery document is empty."
            }
    check(document["issuer"] == issuer) {
      "The discovery document names the issuer ${document["issuer"]} instead of $issuer."
    }
    val builder =
        ClientRegistrations.fromOidcConfiguration(document)
            .registrationId(id.toString())
            .clientId(registration.clientId)
            .scope(registration.scope)
            .redirectUri("{baseUrl}$OIDC_REDIRECT_PATH")
            .clientSettings(
                ClientRegistration.ClientSettings.builder().requireProofKey(true).build())
    if (registration.clientSecret == null) {
      builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
    } else {
      builder.clientSecret(registration.clientSecret)
    }
    return builder.build()
  }

  private fun discoveryRestClient(): RestClient {
    val httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    val requestFactory = JdkClientHttpRequestFactory(httpClient)
    requestFactory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
    return RestClient.builder().requestFactory(requestFactory).build()
  }

  private class LazyRegistration(val id: UUID, val registration: OidcProperties.Registration) {
    @Volatile var resolved: ClientRegistration? = null
    @Volatile var failedAt: Instant? = null
  }
}
