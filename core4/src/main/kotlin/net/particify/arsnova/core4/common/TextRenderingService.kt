/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class TextRenderingService(properties: TextRenderingProperties) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val client =
      RestClient.builder()
          .baseUrl(properties.serviceUrl)
          .defaultHeaders {
            it.accept = listOf(MediaType.APPLICATION_JSON)
            it.contentType = MediaType.APPLICATION_JSON
          }
          .build()

  fun renderText(
      unrenderedText: String?,
      textRenderingOptions: TextRenderingOptions = TextRenderingOptions()
  ): String? {
    if (unrenderedText.isNullOrBlank()) {
      return null
    }

    val requestEntity = RenderingRequestEntity(unrenderedText, textRenderingOptions)
    logger.trace("Sending text to rendering service: {}", unrenderedText.hashCode())
    val responseEntity: RenderingResponseEntity? =
        client
            .post()
            .uri("/render")
            .body(requestEntity)
            .retrieve()
            .body(RenderingResponseEntity::class.java)
    return responseEntity?.html
  }

  private data class RenderingRequestEntity(val text: String, val options: TextRenderingOptions)

  private data class RenderingResponseEntity(var html: String)

  data class TextRenderingOptions(
      @field:JsonProperty("linebreaks") val linebreaksEnabled: Boolean = true,
      @field:JsonProperty("markdown") val markdownEnabled: Boolean = true,
      @field:JsonProperty("latex") val latexEnabled: Boolean = true,
      @field:JsonProperty val markdownFeatureset: MarkdownFeatureset? = MarkdownFeatureset.SIMPLE
  ) {
    enum class MarkdownFeatureset {
      MINIMUM,
      SIMPLE,
      EXTENDED
    }
  }
}
