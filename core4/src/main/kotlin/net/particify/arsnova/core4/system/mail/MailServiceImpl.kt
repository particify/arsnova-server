/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.mail

import jakarta.mail.internet.MimeMessage
import java.io.UnsupportedEncodingException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import net.particify.arsnova.core4.system.MailService
import net.particify.arsnova.core4.system.config.MailProperties
import net.particify.arsnova.core4.system.config.ServiceProperties
import net.particify.arsnova.core4.system.exception.MailAddressTemporarilyBlockedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode

@Service
class MailServiceImpl(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties,
    private val serviceProperties: ServiceProperties,
    private val textTemplateEngine: TemplateEngine,
    private val htmlTemplateEngine: TemplateEngine,
) : MailService {
  @Volatile var freshBlockedAddresses: MutableSet<String> = ConcurrentHashMap.newKeySet()
  @Volatile var staleBlockedAddresses: MutableSet<String> = ConcurrentHashMap.newKeySet()

  override fun sendMail(address: String, template: String, data: Map<String, Any>, locale: Locale) {
    if (!tryBlockAddress(address)) {
      throw MailAddressTemporarilyBlockedException()
    }
    val msg: MimeMessage = mailSender.createMimeMessage()
    val helper = MimeMessageHelper(msg, true, "UTF-8")
    try {
      msg.setHeader("Auto-Submitted", "auto-generated")
      helper.setFrom(mailProperties.senderAddress, mailProperties.senderName)
      helper.setTo(address)
      helper.setSubject(processTemplate("${template}_subject", data, TemplateMode.TEXT, locale))
      helper.setText(
          processTemplate(template, data, TemplateMode.TEXT, locale),
          processTemplate(template, data, TemplateMode.HTML, locale))

      logger.info("Sending mail \"{}\" from \"{}\" to \"{}\"", template, msg.from, address)
      mailSender.send(msg)
    } catch (e: MailException) {
      logger.warn("Mail \"{}\" could not be sent.", template, e)
      throw e
    } catch (e: jakarta.mail.MessagingException) {
      logger.warn("Mail \"{}\" could not be sent because of MessagingException.", template, e)
    } catch (e: UnsupportedEncodingException) {
      logger.error(
          "Mail \"{}\" could not be sent because of the use of an unsupported encoding.",
          template,
          e)
    }
  }

  private fun processTemplate(
      template: String,
      data: Map<String, Any>,
      templateMode: TemplateMode,
      locale: Locale
  ): String {
    val footer =
        mailProperties.footer.find { it.language == locale.language }
            ?: mailProperties.footer.find { it.language == null }
    val context = Context(locale)
    context.setVariable("productName", serviceProperties.productName)
    context.setVariable("serviceUrl", serviceProperties.rootUrl)
    context.setVariable("footer", footer)
    context.setVariables(data)
    val engine =
        when (templateMode) {
          TemplateMode.TEXT -> this.textTemplateEngine
          TemplateMode.HTML -> this.htmlTemplateEngine
          else -> throw IllegalArgumentException("Unsupported TemplateMode")
        }
    val result = engine.process(template, context)
    logger.debug("Processed template {} for data ({}):\n{}", template, data, result)
    return result
  }

  private fun tryBlockAddress(address: String): Boolean {
    return !staleBlockedAddresses.contains(address) && freshBlockedAddresses.add(address)
  }

  /**
   * Clears the list of stale addresses and swaps it with the list of fresh addresses.
   *
   * Address blocking uses two separate lists, one for fresh addresses, which is used for inserting,
   * and one for stale addresses. These lists are swapped after a fixed interval. Since we do not
   * keep track of individual expiration times, the time an address is blocked may vary between the
   * configured number and 2x the number.
   */
  @Scheduled(
      fixedRateString = $$"${mail.address-blocklist-minimum-seconds}", timeUnit = TimeUnit.SECONDS)
  fun clearStaleBlockedAddresses() {
    staleBlockedAddresses.clear()
    val swap = freshBlockedAddresses
    freshBlockedAddresses = staleBlockedAddresses
    staleBlockedAddresses = swap
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(MailServiceImpl::class.java)
  }
}
