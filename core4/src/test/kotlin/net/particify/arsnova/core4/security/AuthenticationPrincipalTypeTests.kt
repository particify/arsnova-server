/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security

import java.lang.reflect.Parameter
import net.particify.arsnova.core4.Core4Application
import net.particify.arsnova.core4.user.AuthenticatedUser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * A principal of another type than the authentication carries is resolved as null rather than
 * rejected, so a mismatching parameter would fail only at runtime, or not at all if it is nullable.
 * Projects building on this one run it against their own controllers as well.
 */
class AuthenticationPrincipalTypeTests {
  @Test
  fun shouldDeclareEveryAuthenticationPrincipalAsAuthenticatedUser() {
    val parameters = controllerClasses().flatMap(::authenticationPrincipalParameters)
    Assertions.assertTrue(parameters.isNotEmpty())
    val mismatches =
        parameters
            .filter { it.type != AuthenticatedUser::class.java }
            .map { "${it.declaringExecutable}: ${it.type.simpleName}" }
    Assertions.assertEquals(emptyList<String>(), mismatches)
  }

  private fun controllerClasses(): List<Class<*>> {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AnnotationTypeFilter(Controller::class.java))
    return scanner.findCandidateComponents(Core4Application::class.java.packageName).map {
      Class.forName(it.beanClassName)
    }
  }

  private fun authenticationPrincipalParameters(controllerClass: Class<*>): List<Parameter> =
      controllerClass.declaredMethods
          .flatMap { it.parameters.asList() }
          .filter { it.isAnnotationPresent(AuthenticationPrincipal::class.java) }
}
