/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import jakarta.servlet.DispatcherType
import java.util.function.Consumer
import net.particify.arsnova.core4.system.security.AuthenticationSuccessHandler
import net.particify.arsnova.core4.system.security.ChallengeJwtAuthenticationFilter
import net.particify.arsnova.core4.system.security.RefreshAuthenticationFilter
import net.particify.arsnova.core4.system.security.UserJwtAuthenticationFilter
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.Saml2ResponseAuthenticationConverter
import org.opensaml.security.x509.X509Support
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint

@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    private val refreshAuthenticationFilter: RefreshAuthenticationFilter,
    private val challengeJwtAuthenticationFilter: ChallengeJwtAuthenticationFilter,
    private val userJwtAuthenticationFilter: UserJwtAuthenticationFilter
) {
  @Bean
  fun filterChain(
      http: HttpSecurity,
      authenticationSuccessHandler: AuthenticationSuccessHandler,
      converter: Saml2ResponseAuthenticationConverter,
      saml2Properties: ExtendedSaml2RelyingPartyProperties,
  ): SecurityFilterChain {
    http
        .csrf(AbstractHttpConfigurer<*, *>::disable)
        .cors(AbstractHttpConfigurer<*, *>::disable)
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .formLogin(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults())
        .authorizeHttpRequests { authorize ->
          authorize
              .dispatcherTypeMatchers(DispatcherType.ERROR)
              .permitAll()
              .requestMatchers("/graphql")
              .authenticated()
              .requestMatchers("/challenge", "/configuration", "/auth/sso/**", "/jwt")
              .permitAll()
              .anyRequest()
              .authenticated()
        }
        .addFilterBefore(
            refreshAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .addFilterBefore(
            challengeJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .addFilterBefore(
            userJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling { it.authenticationEntryPoint(BasicAuthenticationEntryPoint()) }
    if (saml2Properties.registration.isNotEmpty()) {
      val samlAuthenticationProvider = OpenSaml5AuthenticationProvider()
      samlAuthenticationProvider.setResponseAuthenticationConverter(converter)
      http
          .saml2Login {
            it.authenticationManager(ProviderManager(samlAuthenticationProvider))
                .successHandler(authenticationSuccessHandler)
          }
          .saml2Logout(Customizer.withDefaults())
          .saml2Metadata(Customizer.withDefaults())
    }
    return http.build()
  }

  @Bean
  fun authenticationManager(http: HttpSecurity): AuthenticationManager {
    return http.getSharedObject(AuthenticationManagerBuilder::class.java).build()
  }

  @Bean
  fun relyingPartyRegistrations(
      saml2Properties: ExtendedSaml2RelyingPartyProperties
  ): RelyingPartyRegistrationRepository? {
    val registrations =
        saml2Properties.registration.map {
          val credentials =
              it.value.signing.credentials.map { c ->
                require(c.privateKeyLocation != null && c.certificateLocation != null) {
                  "Incomplete SAML configuration ${it.key}"
                }
                val key =
                    RsaKeyConverters.pkcs8().convert(c.privateKeyLocation!!.file.inputStream())
                val certificate = X509Support.decodeCertificate(c.certificateLocation!!.file)
                Saml2X509Credential.signing(key, certificate)
              }
          RelyingPartyRegistrations.fromMetadataLocation(it.value.assertingparty.metadataUri)
              .registrationId(it.key.toString())
              .entityId(it.value.entityId)
              .signingX509Credentials(Consumer { c -> c.addAll(credentials) })
              .build()
        }
    return if (registrations.isNotEmpty()) InMemoryRelyingPartyRegistrationRepository(registrations)
    else null
  }
}
