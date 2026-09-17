/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.saml2.Saml2Exception
import org.springframework.security.saml2.core.Saml2Error
import org.springframework.security.saml2.core.Saml2ErrorCodes
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.OpenSaml5AuthenticationTokenConverter
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher

/** What Spring Boot seeds `acs.location` with when a deployment configures nothing. */
internal const val DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION =
    "{baseUrl}/login/saml2/sso/{registrationId}"

private const val BASE_URL_PLACEHOLDER = "{baseUrl}"

private const val REGISTRATION_ID_PLACEHOLDER = "{registrationId}"

/** Which assertion consumer services the configuration puts a registration behind. */
class Saml2AssertionConsumerServices(
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) {
  /**
   * Where each registration which configures `acs.location` away from
   * [DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION] expects its assertions, keyed by registration.
   * The login DSL serves only the default one, so each of these needs a filter.
   */
  fun configuredPaths(): Map<UUID, String> {
    val paths =
        saml2Properties.registration
            .filterValues { it.acs.location != DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION }
            .mapValues { (registrationId, registration) ->
              assertionConsumerServicePath(registrationId, registration.acs.location)
            }
    val shared = paths.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }
    require(shared.isEmpty()) {
      "Conflicting SAML configuration: an assertion consumer service carries no registration ID, " +
          "so registrations cannot share one: $shared."
    }
    return paths
  }

  /**
   * The locations [registration] accepts, resolved against the same base URL Spring resolved the
   * primary one with. Neither a response validator nor the metadata customizer sees the request, so
   * the base URL is recovered from the primary location rather than built from scratch. A primary
   * location which does not end in the path it was configured with was resolved by something this
   * does not model, and then the primary one is all that can safely be accepted.
   */
  fun acceptedLocations(registration: RelyingPartyRegistration): List<String> {
    val location = registration.assertionConsumerServiceLocation
    val configured =
        runCatching { UUID.fromString(registration.registrationId) }
            .getOrNull()
            ?.let { saml2Properties.registration[it] } ?: return listOf(location)
    val locations = assertionConsumerServiceLocations(configured)
    val primaryPath = servedPath(locations.first(), registration.registrationId)
    if (!location.endsWith(primaryPath)) {
      return listOf(location)
    }
    val baseUrl = location.removeSuffix(primaryPath)
    return locations.map { baseUrl + servedPath(it, registration.registrationId) }
  }
}

/**
 * Serves every assertion consumer service a registration configures beyond Spring's default,
 * alongside the one the login DSL sets up.
 *
 * A filter of its own is the only way to serve one next to the DSL's:
 * `Saml2LoginConfigurer.loginProcessingUrl` replaces the default rather than adding to it. Nothing
 * wires a hand-added filter, so everything `AbstractAuthenticationFilterConfigurer` does for the
 * DSL's own filter is repeated below.
 *
 * The filters need no `permitAll` entry: each short-circuits the chain on a match, so
 * `AuthorizationFilter` is never reached.
 */
class Saml2AssertionConsumerServiceConfigurer(
    private val registrations: RelyingPartyRegistrationRepository,
    saml2Properties: ExtendedSaml2RelyingPartyProperties,
    private val authenticationManager: AuthenticationManager,
    private val authenticationSuccessHandler: AuthenticationSuccessHandler,
    private val authenticationFailureHandler: AuthenticationFailureHandler,
    private val authenticationRequestRepository:
        Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest>
) : AbstractHttpConfigurer<Saml2AssertionConsumerServiceConfigurer, HttpSecurity>() {
  private val assertionConsumerServices = Saml2AssertionConsumerServices(saml2Properties)

  override fun configure(http: HttpSecurity) {
    for (path in assertionConsumerServices.configuredPaths().values) {
      http.addFilterBefore(
          assertionConsumerServiceFilter(http, path), Saml2WebSsoAuthenticationFilter::class.java)
    }
  }

  private fun assertionConsumerServiceFilter(
      http: HttpSecurity,
      path: String
  ): Saml2WebSsoAuthenticationFilter {
    val matcher = PathPatternRequestMatcher.pathPattern(path)
    // Without a registrationId in the path, resolution falls back to the asserting party's entity
    // ID, which the stock converter cannot do.
    val converter = OpenSaml5AuthenticationTokenConverter(registrations)
    converter.setRequestMatcher(matcher)
    converter.setAuthenticationRequestRepository(authenticationRequestRepository)
    val filter =
        Saml2WebSsoAuthenticationFilter(FailureTranslatingAuthenticationConverter(converter), path)
    filter.setAuthenticationManager(authenticationManager)
    filter.setAuthenticationSuccessHandler(authenticationSuccessHandler)
    filter.setAuthenticationFailureHandler(authenticationFailureHandler)
    filter.setAuthenticationRequestRepository(authenticationRequestRepository)
    filter.setSecurityContextHolderStrategy(securityContextHolderStrategy)
    filter.setSessionAuthenticationStrategy(NullAuthenticatedSessionStrategy())
    http
        .getSharedObject(SecurityContextRepository::class.java)
        ?.let(filter::setSecurityContextRepository)
    return filter
  }
}

/**
 * Reports a response the delegate cannot read as a failed authentication rather than by throwing
 * out of the filter, which answers with the container's error page instead of the failure handler.
 *
 * Only the entity ID fallback needs this, and a path carrying no registration ID always takes it:
 * it states its expectation of an `Issuer` with `Assert.notNull` rather than by returning null, it
 * lets a payload which does not deserialize surface as a [Saml2Exception], and it casts to a
 * `Response` whatever did deserialize. Those three are translated and nothing else, because
 * anything further is a defect rather than an unreadable request and has to keep surfacing as one.
 */
private class FailureTranslatingAuthenticationConverter(
    private val delegate: AuthenticationConverter
) : AuthenticationConverter {
  override fun convert(request: HttpServletRequest): Authentication? =
      try {
        delegate.convert(request)
      } catch (e: IllegalArgumentException) {
        throw unreadableResponse(e)
      } catch (e: ClassCastException) {
        throw unreadableResponse(e)
      } catch (e: Saml2Exception) {
        throw unreadableResponse(e)
      }

  private fun unreadableResponse(cause: RuntimeException) =
      Saml2AuthenticationException(
          Saml2Error(Saml2ErrorCodes.INVALID_RESPONSE, "Unreadable SAML response"), cause)
}

/**
 * Every assertion consumer service a registration accepts, primary first.
 * [DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION] is always among them, so a registration serving a
 * location of its own stays reachable at the one every other deployment uses, and moving from one
 * to the other needs no window in which only one of them works.
 */
private fun assertionConsumerServiceLocations(registration: ExtendedRegistration): List<String> =
    listOf(registration.acs.location, DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION).distinct()

/**
 * The servlet context path is part of `{baseUrl}`, so it must not reach a filter's matcher. An
 * absolute location cannot say where the context path ends, which is why only the placeholder form
 * is accepted.
 */
private fun assertionConsumerServicePath(registrationId: UUID, location: String): String {
  require(location.startsWith("$BASE_URL_PLACEHOLDER/")) {
    "Unsupported SAML configuration $registrationId: " +
        "security.saml2.relyingparty.registration.$registrationId.acs.location has to start " +
        "with $BASE_URL_PLACEHOLDER/ to be served."
  }
  return servedPath(location, registrationId.toString())
}

/** What [location] leaves once the base URL is stripped and the registration is filled in. */
private fun servedPath(location: String, registrationId: String) =
    location.removePrefix(BASE_URL_PLACEHOLDER).replace(REGISTRATION_ID_PLACEHOLDER, registrationId)
