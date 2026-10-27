/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val TEXT = "format,body\nTEXT,Grüße"

class DelimitedTextCharsetsTest {
  @Test
  fun shouldMarkOnlyUtf16LeWithAByteOrderMark() {
    val utf16 = DelimitedTextCharsets.encode(TEXT, Charsets.UTF_16LE)
    Assertions.assertEquals(listOf(0xFF.toByte(), 0xFE.toByte()), utf16.take(2))
    Assertions.assertArrayEquals(
        TEXT.toByteArray(Charsets.UTF_8), DelimitedTextCharsets.encode(TEXT, Charsets.UTF_8))
  }

  @Test
  fun shouldDecodeByTheByteOrderMark() {
    val utf16Be = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + TEXT.toByteArray(Charsets.UTF_16BE)
    val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + TEXT.toByteArray()
    listOf(
            DelimitedTextCharsets.encode(TEXT, Charsets.UTF_16LE),
            utf16Be,
            utf8Bom,
            TEXT.toByteArray(Charsets.UTF_8))
        .forEach { Assertions.assertEquals(TEXT, DelimitedTextCharsets.decode(it)) }
    Assertions.assertEquals("", DelimitedTextCharsets.decode(byteArrayOf()))
  }

  @Test
  fun shouldReadParametersCaseInsensitively() {
    Assertions.assertEquals(Charsets.UTF_16LE, DelimitedTextCharsets.fromParameter("utf-16le"))
    Assertions.assertEquals(Charsets.UTF_8, DelimitedTextCharsets.fromParameter("UTF-8"))
    Assertions.assertNull(DelimitedTextCharsets.fromParameter("ISO-8859-1"))
    Assertions.assertEquals(DelimitedTextFormat.TSV, DelimitedTextFormat.fromParameter("tsv"))
    Assertions.assertEquals(DelimitedTextFormat.CSV, DelimitedTextFormat.fromParameter("CSV"))
    Assertions.assertNull(DelimitedTextFormat.fromParameter("xlsx"))
  }
}
