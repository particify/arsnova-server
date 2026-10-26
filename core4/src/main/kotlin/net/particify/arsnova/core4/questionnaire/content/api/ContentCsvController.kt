/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.common.DelimitedTextCharsets
import net.particify.arsnova.core4.common.DelimitedTextFormat
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.content.ContentCsvImportResult
import net.particify.arsnova.core4.questionnaire.content.ContentCsvService
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Exports and imports a questionnaire's contents as files, which is why this is REST rather than
 * GraphQL: the charset and the byte order mark of a file are decided here, as v3 did.
 */
@RestController
@PreAuthorize("hasRole('USER')")
class ContentCsvController(private val contentCsvService: ContentCsvService) {
  @GetMapping("/questionnaire/{questionnaireId}/contents-export")
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun exportContents(
      @PathVariable questionnaireId: UUID,
      @RequestParam(defaultValue = "csv") format: String,
      @RequestParam(defaultValue = "UTF-8") charset: String,
      @RequestParam contentIds: List<UUID>?
  ): ResponseEntity<ByteArray> {
    val textFormat =
        DelimitedTextFormat.fromParameter(format)
            ?: throw InvalidInputException("Unsupported format: $format")
    val textCharset =
        DelimitedTextCharsets.fromParameter(charset)
            ?: throw InvalidInputException("Unsupported charset: $charset")
    val text = contentCsvService.export(questionnaireId, contentIds, textFormat)
    val disposition =
        ContentDisposition.attachment().filename("contents.${textFormat.extension}").build()
    return ResponseEntity.ok()
        .contentType(MediaType(textFormat.mediaType, textCharset))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(DelimitedTextCharsets.encode(text, textCharset))
  }

  /** The separator is detected from the header, the charset from the byte order mark. */
  @PostMapping(
      "/questionnaire/{questionnaireId}/contents-import",
      consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun importContents(
      @PathVariable questionnaireId: UUID,
      @RequestPart file: MultipartFile
  ): ContentCsvImportResult =
      contentCsvService.import(questionnaireId, DelimitedTextCharsets.decode(file.bytes))
}
