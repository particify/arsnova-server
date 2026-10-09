/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.particify.arsnova.core4.common.web.htmlErrorPage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.View

private const val CONTENT_TYPE = "text/html;charset=UTF-8"

private const val SUMMARY = "The request could not be completed."

/**
 * Takes the place of Spring Boot's Whitelabel page, which is registered only while no bean of this
 * name exists. JSON clients are unaffected: they never reach a view.
 */
@Configuration
class ErrorViewConfiguration {
  @Bean("error") fun errorView(): View = HtmlErrorView()
}

/** Renders the status alone, so nothing an exception carries reaches the page. */
private class HtmlErrorView : View {
  override fun getContentType(): String = CONTENT_TYPE

  override fun render(
      model: Map<String, *>?,
      request: HttpServletRequest,
      response: HttpServletResponse
  ) {
    val status =
        (model?.get("status") as? Int)?.let(HttpStatus::resolve) ?: HttpStatus.INTERNAL_SERVER_ERROR
    response.contentType = CONTENT_TYPE
    response.writer.write(htmlErrorPage(status, SUMMARY))
  }
}
