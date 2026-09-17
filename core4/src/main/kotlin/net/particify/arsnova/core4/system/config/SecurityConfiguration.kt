/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import jakarta.servlet.DispatcherType
import net.particify.arsnova.core4.system.security.AuthenticationSuccessHandler
import net.particify.arsnova.core4.system.security.ChallengeJwtAuthenticationFilter
import net.particify.arsnova.core4.system.security.Http401UnauthenticatedEntryPoint
import net.particify.arsnova.core4.system.security.RefreshAuthenticationFilter
import net.particify.arsnova.core4.system.security.RefreshableRelyingPartyRegistrationRepository
import net.particify.arsnova.core4.system.security.Saml2AssertionConsumerServiceConfigurer
import net.particify.arsnova.core4.system.security.UserJwtAuthenticationFilter
import net.particify.arsnova.core4.user.ADMIN_ROLE
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.Saml2ResponseAuthenticationConverter
import org.slf4j.LoggerFactory
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.SecurityConfigurerAdapter
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.HttpSessionSaml2AuthenticationRequestRepository
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * What the SAML login DSL redirects a failed authentication to. Stated rather than left to the DSL,
 * because the assertion consumer services served next to it have to answer the same way and take
 * the handler built from this.
 */
private const val SAML_LOGIN_FAILURE_URL = "/login?error"

private val logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)

@Configuration
@EnableWebSecurity
@Suppress("LongMethod")
class SecurityConfiguration(
    private val refreshAuthenticationFilter: RefreshAuthenticationFilter,
    private val challengeJwtAuthenticationFilter: ChallengeJwtAuthenticationFilter,
    private val userJwtAuthenticationFilter: UserJwtAuthenticationFilter
) {
  @Bean
  fun filterChain(
      environment: Environment,
      http: HttpSecurity,
      authenticationSuccessHandler: AuthenticationSuccessHandler,
      converter: Saml2ResponseAuthenticationConverter,
      saml2Properties: ExtendedSaml2RelyingPartyProperties,
      relyingPartyRegistrations: RelyingPartyRegistrationRepository?,
      saml2AuthenticationRequestRepository:
          Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest>,
      publicRoutesList: List<PublicRoutes>,
      configurers: List<SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>>
  ): SecurityFilterChain {
    val publicRoutes = publicRoutesList.flatMap { it.routes }
    if (publicRoutes.isNotEmpty()) {
      logger.debug("Additional public routes: {}", publicRoutes)
    }
    if (environment.activeProfiles.contains("dev")) {
      http.httpBasic(Customizer.withDefaults()).authorizeHttpRequests { authorize ->
        authorize.requestMatchers("/graphiql").permitAll()
      }
    }
    for (configurer in configurers) {
      http.with(configurer, Customizer.withDefaults())
    }
    http
        .csrf(AbstractHttpConfigurer<*, *>::disable)
        .cors(AbstractHttpConfigurer<*, *>::disable)
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { authorize ->
          authorize
              .dispatcherTypeMatchers(DispatcherType.ERROR)
              .permitAll()
              .requestMatchers("/graphql")
              .authenticated()
              .requestMatchers(
                  "/auth/sso/**",
                  "/challenge",
                  "/configuration",
                  "/graphql/ws",
                  "/jwt",
                  *publicRoutes.toTypedArray())
              .permitAll()
              .requestMatchers(EndpointRequest.to("health"))
              .permitAll()
              .requestMatchers(EndpointRequest.to("info", "metrics", "prometheus"))
              .hasAnyRole(ADMIN_ROLE, "OBSERVABILITY")
              .requestMatchers(EndpointRequest.toAnyEndpoint())
              .hasRole(ADMIN_ROLE)
        }
        .addFilterBefore(
            refreshAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .addFilterBefore(
            challengeJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .addFilterBefore(
            userJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling { it.authenticationEntryPoint(Http401UnauthenticatedEntryPoint()) }
    // Use separate Configurer to ensure that the catch-all is executed last after other
    // Configurers.
    http.with(
        object : AbstractHttpConfigurer<Nothing, HttpSecurity>() {
          override fun configure(http: HttpSecurity) {
            http.authorizeHttpRequests { it.anyRequest().authenticated() }
          }
        })
    if (saml2Properties.registration.isNotEmpty()) {
      val samlAuthenticationProvider = OpenSaml5AuthenticationProvider()
      samlAuthenticationProvider.setResponseAuthenticationConverter(converter)
      val samlAuthenticationManager = ProviderManager(samlAuthenticationProvider)
      // One handler for both, so a change to how a failed login is answered lands once.
      val samlAuthenticationFailureHandler =
          SimpleUrlAuthenticationFailureHandler(SAML_LOGIN_FAILURE_URL)
      http
          .saml2Login {
            it.authenticationManager(samlAuthenticationManager)
                .successHandler(authenticationSuccessHandler)
                .failureHandler(samlAuthenticationFailureHandler)
          }
          .saml2Logout(Customizer.withDefaults())
          .saml2Metadata(Customizer.withDefaults())
          .with(
              Saml2AssertionConsumerServiceConfigurer(
                  checkNotNull(relyingPartyRegistrations),
                  saml2Properties,
                  samlAuthenticationManager,
                  authenticationSuccessHandler,
                  samlAuthenticationFailureHandler,
                  saml2AuthenticationRequestRepository),
              Customizer.withDefaults())
    }
    return http.build()
  }

  @Bean
  fun authenticationManager(http: HttpSecurity): AuthenticationManager {
    return http.getSharedObject(AuthenticationManagerBuilder::class.java).build()
  }

  /**
   * Shared, so the legacy assertion consumer service finds the authentication request the modern
   * one stored. Two instances would reject every response carrying an `InResponseTo`.
   */
  @Bean
  fun saml2AuthenticationRequestRepository():
      Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> =
      HttpSessionSaml2AuthenticationRequestRepository()

  @Bean
  fun relyingPartyRegistrations(
      saml2Properties: ExtendedSaml2RelyingPartyProperties
  ): RelyingPartyRegistrationRepository? =
      if (saml2Properties.registration.isEmpty()) null
      else RefreshableRelyingPartyRegistrationRepository(saml2Properties)
}
