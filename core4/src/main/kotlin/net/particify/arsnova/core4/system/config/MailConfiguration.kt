/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import net.particify.arsnova.common.util.YamlPropertiesLoader
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.thymeleaf.TemplateEngine
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode

@Configuration
class MailConfiguration {
  @Bean
  fun mailSender(mailProperties: MailProperties): JavaMailSenderImpl {
    val mailSender = JavaMailSenderImpl()
    if (mailProperties.implicitTls) {
      mailSender.javaMailProperties.setProperty("mail.transport.protocol", "smtps")
    }
    mailSender.host = mailProperties.host
    if (mailProperties.port > 0) {
      mailSender.port = mailProperties.port
    }
    if (!mailProperties.username.isNullOrEmpty() && !mailProperties.password.isNullOrEmpty()) {
      mailSender.username = mailProperties.username
      mailSender.password = mailProperties.password
      mailSender.javaMailProperties.setProperty("mail.smtp.auth", "true")
      mailSender.javaMailProperties.setProperty("mail.smtps.auth", "true")
    }
    return mailSender
  }

  fun textTemplateResolver(applicationContext: ApplicationContext): SpringResourceTemplateResolver {
    val resolver = SpringResourceTemplateResolver()
    resolver.setApplicationContext(applicationContext)
    resolver.prefix = "classpath:/templates/mail/"
    resolver.suffix = ".txt"
    resolver.templateMode = TemplateMode.TEXT
    return resolver
  }

  @Bean
  fun textTemplateEngine(applicationContext: ApplicationContext): TemplateEngine {
    val engine = SpringTemplateEngine()
    engine.setTemplateResolver(textTemplateResolver(applicationContext))
    engine.setTemplateEngineMessageSource(mailMessageSource())
    return engine
  }

  fun htmlTemplateResolver(applicationContext: ApplicationContext): SpringResourceTemplateResolver {
    val resolver = SpringResourceTemplateResolver()
    resolver.setApplicationContext(applicationContext)
    resolver.prefix = "classpath:/templates/mail/"
    resolver.suffix = ".html"
    resolver.templateMode = TemplateMode.HTML
    return resolver
  }

  @Bean
  fun htmlTemplateEngine(applicationContext: ApplicationContext): TemplateEngine {
    val engine = SpringTemplateEngine()
    engine.setTemplateResolver(htmlTemplateResolver(applicationContext))
    engine.setTemplateEngineMessageSource(mailMessageSource())
    return engine
  }

  @Bean
  fun mailMessageSource(): MessageSource {
    val messageSource = ReloadableResourceBundleMessageSource()
    messageSource.setBasename("classpath:messages/mail")
    messageSource.setPropertiesPersister(YamlPropertiesLoader())
    messageSource.setFileExtensions(mutableListOf(".yml", ".yaml"))
    return messageSource
  }
}
