/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.internal.api

import java.util.Locale
import net.particify.arsnova.core4.common.LANGUAGE_ISO_639_CODES
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class LanguageQueryController {
  private val languages =
      LANGUAGE_ISO_639_CODES.map {
        val locale = Locale.of(it)
        Language(it, locale.getDisplayLanguage(locale))
      }

  @QueryMapping
  fun languages(): List<Language> {
    return languages
  }
}
