/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import jakarta.servlet.DispatcherType
import net.particify.arsnova.core4.system.security.JwtAuthenticationFilter
import net.particify.arsnova.core4.user.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint

@Configuration
@EnableWebSecurity
class SecurityConfiguration(private val jwtAuthenticationFilter: JwtAuthenticationFilter) {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .csrf(AbstractHttpConfigurer<*, *>::disable)
        .cors(AbstractHttpConfigurer<*, *>::disable)
        .formLogin(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults())
        .authorizeHttpRequests { authorize ->
          authorize
              .dispatcherTypeMatchers(DispatcherType.ERROR)
              .permitAll()
              .requestMatchers("/graphql")
              .authenticated()
              .requestMatchers("/configuration", "/auth/login/**", "/jwt")
              .permitAll()
              .anyRequest()
              .authenticated()
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling { it.authenticationEntryPoint(BasicAuthenticationEntryPoint()) }
    return http.build()
  }

  @Bean
  fun authenticationManager(http: HttpSecurity): AuthenticationManager {
    return http.getSharedObject(AuthenticationManagerBuilder::class.java).build()
  }

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
