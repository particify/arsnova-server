/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.buildlogic

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

private const val TAR_BLOCK = 512
private const val TAR_SIZE_OFFSET = 124
private const val TAR_CHECKSUM_OFFSET = 148
private const val TAR_TYPE_OFFSET = 156
private const val TAR_MAGIC_OFFSET = 257
private const val TAR_CHECKSUM_LENGTH = 8
private const val TAR_NAME_LENGTH = 100

private const val PACKAGE_JSON = """{"name": "@zxcvbn-ts/language-xx-yy", "version": "1.2.3"}"""
private const val LICENSE = "MIT License"

private const val INDEX =
    """import commonWords from './commonWords.json'
import wordSequences from './wordSequences.json'

const dictionary = {
  'commonWords-xx': commonWords,
  ...wordSequences,
}
"""

/** Entries which must neither be read nor be written anywhere. */
private val MALICIOUS_ENTRIES =
    listOf(
        "../escape-relative.txt" to "evil",
        "/tmp/escape-absolute.txt" to "evil",
        "package/../../escape-dotdot.txt" to "evil",
        "package/../src/index.ts" to "const dictionary = { 'shadowed-xx': commonWords }",
        "x/src/index.ts" to "const dictionary = { 'shadowed-xx': commonWords }")

class PasswordDictionaryPackageTest {
  @TempDir lateinit var temp: Path

  @Test
  fun shouldConvertPackage() {
    val root = convert(validEntries())
    val dictionaries = File(root, "dictionaries/xx")
    Assertions.assertEquals(
        "commonWords-xx commonWords-xx.txt\nmonths-xx months-xx.txt\n",
        File(dictionaries, "index.txt").readText())
    Assertions.assertEquals("alpha\nbeta\n", File(dictionaries, "commonWords-xx.txt").readText())
    Assertions.assertEquals("one\ntwo\n", File(dictionaries, "months-xx.txt").readText())
    Assertions.assertEquals(
        "@zxcvbn-ts/language-xx-yy 1.2.3", File(root, "notices/package.txt").readText())
    Assertions.assertEquals(LICENSE, File(root, "notices/LICENSE.txt").readText())
    Assertions.assertEquals(
        "Attribution\n\nThird party", File(root, "notices/attribution.md").readText())
  }

  @Test
  fun shouldReadOnlyNeededEntries() {
    val entries =
        PasswordDictionaryPackage.readTarball(
            tarball(MALICIOUS_ENTRIES + validEntries()).inputStream())
    Assertions.assertEquals(
        setOf(
            "package.json",
            "LICENSE.txt",
            "NOTICE.md",
            "THIRD_PARTY_LICENSES.md",
            "src/index.ts",
            "src/commonWords.json",
            "src/wordSequences.json"),
        entries.keys)
    Assertions.assertEquals(INDEX, entries["src/index.ts"])
  }

  @Test
  fun shouldWriteNothingForPathsOfEntries() {
    val root = convert(MALICIOUS_ENTRIES + validEntries())
    Assertions.assertEquals(
        "commonWords-xx commonWords-xx.txt\nmonths-xx months-xx.txt\n",
        File(root, "dictionaries/xx/index.txt").readText())
    assertNoEscape()
  }

  @Test
  fun shouldRefuseUnsafeDictionaryName() {
    val entries =
        validEntries().map { (path, content) ->
          path to
              if (path == "package/src/index.ts")
                  content.replace("'commonWords-xx'", "'../../escape-name'")
              else content
        }
    val exception = assertThrows<GradleException> { convert(entries) }
    Assertions.assertTrue(exception.message!!.contains("../../escape-name"), exception.message)
    Assertions.assertFalse(File(temp.toFile(), "output/dictionaries").exists())
    assertNoEscape()
  }

  @Test
  fun shouldRefuseUnsafeLanguage() {
    val entries =
        validEntries().map { (path, content) ->
          path to
              if (path == "package/package.json")
                  content.replace("language-xx-yy", "language-../../escape")
              else content
        }
    assertThrows<GradleException> { convert(entries) }
    assertNoEscape()
  }

  private fun convert(entries: List<Pair<String, String>>): File {
    val root = File(temp.toFile(), "work/output")
    PasswordDictionaryPackage.convert(tarball(entries).inputStream(), "language-xx-1.2.3.tgz", root)
    return root
  }

  /** The output lies two levels below the temporary directory, so an escape would land in it. */
  private fun assertNoEscape() {
    val escaped = temp.toFile().walk().filter { it.isFile && it.name.startsWith("escape") }.toList()
    Assertions.assertEquals(listOf<File>(), escaped)
    Assertions.assertFalse(File("/tmp/escape-absolute.txt").exists())
  }

  private fun validEntries() =
      listOf(
          "package/package.json" to PACKAGE_JSON,
          "package/LICENSE.txt" to LICENSE,
          "package/NOTICE.md" to "Attribution\n",
          "package/THIRD_PARTY_LICENSES.md" to "Third party\n",
          "package/README.md" to "Not needed",
          "package/src/index.ts" to INDEX,
          "package/src/commonWords.json" to """["alpha", "beta"]""",
          "package/src/wordSequences.json" to """{"months-xx": ["one", "two"]}""")

  private fun tarball(entries: List<Pair<String, String>>): ByteArray {
    val bytes = ByteArrayOutputStream()
    GZIPOutputStream(bytes).use { gzip ->
      entries.forEach { (path, content) ->
        val data = content.toByteArray()
        gzip.write(header(path, data.size))
        gzip.write(data)
        gzip.write(ByteArray((TAR_BLOCK - data.size % TAR_BLOCK) % TAR_BLOCK))
      }
      gzip.write(ByteArray(2 * TAR_BLOCK))
    }
    return bytes.toByteArray()
  }

  private fun header(path: String, size: Int): ByteArray {
    val header = ByteArray(TAR_BLOCK)
    path.toByteArray().copyInto(header, endIndex = minOf(path.length, TAR_NAME_LENGTH))
    "%011o".format(size).toByteArray().copyInto(header, TAR_SIZE_OFFSET)
    header[TAR_TYPE_OFFSET] = '0'.code.toByte()
    "ustar\u000000".toByteArray().copyInto(header, TAR_MAGIC_OFFSET)
    " ".repeat(TAR_CHECKSUM_LENGTH).toByteArray().copyInto(header, TAR_CHECKSUM_OFFSET)
    val checksum = header.sumOf { it.toInt() and 0xff }
    "%06o\u0000 ".format(checksum).toByteArray().copyInto(header, TAR_CHECKSUM_OFFSET)
    return header
  }
}
