/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.web

import org.springframework.http.HttpStatus
import org.springframework.web.util.HtmlUtils

/**
 * A self-contained page for an error a browser has navigated to, where no frontend is around to
 * present it. [detail] is left out when null, and so is the Close button running [closeScript].
 *
 * [closeScript] is inserted as JavaScript and therefore not escaped, so it has to be a constant.
 */
fun htmlErrorPage(
    status: HttpStatus,
    summary: String,
    detail: String? = null,
    closeScript: String? = null
): String {
  val title = "${status.value()} ${HtmlUtils.htmlEscape(status.reasonPhrase)}"
  return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>$title</title></head><body>" +
      "<h1>$title</h1><p>${HtmlUtils.htmlEscape(summary)}</p>" +
      detail?.let { "<p>${HtmlUtils.htmlEscape(it)}</p>" }.orEmpty() +
      closeScript?.let(::closeButton).orEmpty() +
      "</body></html>"
}

private fun closeButton(closeScript: String): String =
    "<button id=\"close\" type=\"button\">Close</button><script>" +
        "document.getElementById('close').addEventListener('click', () => $closeScript)" +
        "</script>"
