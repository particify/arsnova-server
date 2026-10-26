/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import java.nio.charset.Charset

/**
 * The charsets delimited text files are exchanged in, with v3's byte order mark rule: spreadsheets
 * need one to tell UTF-16 apart, and some of them misread UTF-8 text which has one.
 */
object DelimitedTextCharsets {
  private val UTF_16LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
  private val UTF_16BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
  private val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

  /** The charsets a file can be exported in. */
  val SUPPORTED: List<Charset> = listOf(Charsets.UTF_8, Charsets.UTF_16LE)

  fun fromParameter(value: String): Charset? =
      SUPPORTED.find { it.name().equals(value, ignoreCase = true) }

  /** Prepends the byte order mark for UTF-16LE only. */
  fun encode(text: String, charset: Charset): ByteArray {
    val bytes = text.toByteArray(charset)
    return if (charset == Charsets.UTF_16LE) UTF_16LE_BOM + bytes else bytes
  }

  /** Reads UTF-16 by its byte order mark and anything else as UTF-8, without the mark. */
  fun decode(bytes: ByteArray): String =
      when {
        bytes.startsWith(UTF_16LE_BOM) ->
            String(bytes, UTF_16LE_BOM.size, bytes.size - UTF_16LE_BOM.size, Charsets.UTF_16LE)
        bytes.startsWith(UTF_16BE_BOM) ->
            String(bytes, UTF_16BE_BOM.size, bytes.size - UTF_16BE_BOM.size, Charsets.UTF_16BE)
        bytes.startsWith(UTF_8_BOM) ->
            String(bytes, UTF_8_BOM.size, bytes.size - UTF_8_BOM.size, Charsets.UTF_8)
        else -> String(bytes, Charsets.UTF_8)
      }

  private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
      size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
