/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.mail.internal

import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Locale
import java.util.Properties
import net.particify.arsnova.core4.common.ServiceProperties
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.mail.config.MailProperties
import net.particify.arsnova.core4.mail.exception.MailAddressTemporarilyBlockedException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templateresolver.StringTemplateResolver

private const val TEMPLATE = "template"

class MailServiceImplTests {
  private val mailSender = Mockito.mock(JavaMailSender::class.java)
  private val mailService =
      MailServiceImpl(
          mailSender,
          MailProperties(senderAddress = "sender@example.com", senderName = "Sender"),
          ServiceProperties(productName = "Product", rootUrl = "https://example.com"),
          templateEngine(),
          templateEngine())

  init {
    Mockito.`when`(mailSender.createMimeMessage()).thenAnswer {
      MimeMessage(Session.getInstance(Properties()))
    }
  }

  @Test
  fun shouldSendToBareAddress() {
    mailService.sendMail("recipient@example.com", TEMPLATE, mapOf(), Locale.ENGLISH)
    val message = ArgumentCaptor.forClass(MimeMessage::class.java)
    Mockito.verify(mailSender).send(message.capture())
    Assertions.assertArrayEquals(
        arrayOf(InternetAddress("recipient@example.com")), message.value.allRecipients)
  }

  /**
   * JavaMail accepts each of these as a recipient list, and would deliver to an address other than
   * the one passed in, or to more than one.
   */
  @ParameterizedTest
  @ValueSource(
      strings =
          [
              "'My Name' <name@example.com>",
              "<x@example.com> a@example.org",
              "a@example.org (comment)",
              "a@example.org, b@example.org",
              "mail@example.com>",
              " padded@example.com"])
  fun shouldRejectRecipientOtherThanBareAddress(address: String) {
    assertThrows<InvalidInputException> {
      mailService.sendMail(address, TEMPLATE, mapOf(), Locale.ENGLISH)
    }
    Mockito.verify(mailSender, Mockito.never()).send(Mockito.any(MimeMessage::class.java))
  }

  @Test
  fun shouldBlockRepeatedMailRegardlessOfCase() {
    mailService.sendMail("Repeated@Example.com", TEMPLATE, mapOf(), Locale.ENGLISH)
    assertThrows<MailAddressTemporarilyBlockedException> {
      mailService.sendMail("repeated@example.com", TEMPLATE, mapOf(), Locale.ENGLISH)
    }
  }

  /** Resolves a template name as the template itself, so no template files are needed. */
  private fun templateEngine() =
      SpringTemplateEngine().apply { setTemplateResolver(StringTemplateResolver()) }
}
