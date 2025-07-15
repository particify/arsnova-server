/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import net.particify.arsnova.core4.user.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AuthenticationProviderConfiguration {
  @Bean
  fun daoAuthenticationProvider(userDetailsService: UserService): DaoAuthenticationProvider {
    val provider = DaoAuthenticationProvider(userDetailsService)
    provider.setPasswordEncoder(passwordEncoder())
    return provider
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    val encoders = mapOf("bcrypt" to BCryptPasswordEncoder())
    return DelegatingPasswordEncoder("bcrypt", encoders)
  }
}
