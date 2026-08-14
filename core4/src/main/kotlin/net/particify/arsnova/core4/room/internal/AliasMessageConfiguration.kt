/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import net.particify.arsnova.common.util.YamlPropertiesLoader
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource

@Configuration
class AliasMessageConfiguration {
  @Bean
  fun aliasMessageSource(): MessageSource {
    val messageSource = ReloadableResourceBundleMessageSource()
    messageSource.setBasename("classpath:messages/alias")
    messageSource.setPropertiesPersister(YamlPropertiesLoader())
    messageSource.setFileExtensions(mutableListOf(".yml", ".yaml"))
    // Aliases are localized for the request's locale, not the system's.
    messageSource.setFallbackToSystemLocale(false)
    return messageSource
  }
}
