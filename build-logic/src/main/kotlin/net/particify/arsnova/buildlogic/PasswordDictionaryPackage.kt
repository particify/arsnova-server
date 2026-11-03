/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.buildlogic

import groovy.json.JsonSlurper
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream
import org.gradle.api.GradleException

private const val TAR_BLOCK = 512
private const val TAR_NAME_LENGTH = 100
private const val TAR_SIZE_OFFSET = 124
private const val TAR_SIZE_LENGTH = 12
private const val TAR_TYPE_OFFSET = 156
private const val TAR_PREFIX_OFFSET = 345
private const val TAR_PREFIX_LENGTH = 155
private const val OCTAL = 8
private const val PACKAGE_DIRECTORY = "package/"
private val NEEDED_ENTRY =
    Regex(
        "package/(package\\.json|LICENSE\\.txt|NOTICE\\.md|THIRD_PARTY_LICENSES\\.md|" +
            "src/index\\.ts|src/\\w+\\.json)")
private val SAFE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9-]*")

/**
 * Converts a zxcvbn-ts language package into a directory named by the language's ISO 639-1 code,
 * holding one file per dictionary and an index naming them, plus the package's notices.
 *
 * Only the entries it needs are read from the tarball, into memory, and no output path is built
 * from an entry's path. Names taken from the package's content are checked before they name a file.
 */
internal object PasswordDictionaryPackage {
  fun convert(tarball: InputStream, tarballName: String, root: File) {
    val entries = readTarball(tarball)
    @Suppress("UNCHECKED_CAST")
    val packageJson = JsonSlurper().parseText(entries.getValue("package.json")) as Map<String, Any>
    val packageName = packageJson["name"] as String
    val language =
        checkedName(
            tarballName, packageName.removePrefix("@zxcvbn-ts/language-").substringBefore("-"))
    writeDictionaries(tarballName, entries, File(root, "dictionaries/$language"))
    val notices = File(root, "notices").apply { mkdirs() }
    File(notices, "package.txt").writeText("$packageName ${packageJson["version"]}")
    File(notices, "LICENSE.txt").writeText(entries.getValue("LICENSE.txt"))
    val attribution =
        listOf("NOTICE.md", "THIRD_PARTY_LICENSES.md")
            .mapNotNull { entries[it]?.trim() }
            .joinToString("\n\n")
    if (attribution.isNotEmpty()) {
      File(notices, "attribution.md").writeText(attribution)
    }
  }

  /**
   * The needed text files of the tarball, by their path below the package's root directory. Every
   * other entry, including any whose path leaves that directory, is skipped unread.
   */
  fun readTarball(input: InputStream): Map<String, String> {
    val entries = mutableMapOf<String, String>()
    GZIPInputStream(input).use { stream ->
      val header = ByteArray(TAR_BLOCK)
      while (stream.readNBytes(header, 0, TAR_BLOCK) == TAR_BLOCK && header[0] != 0.toByte()) {
        val name = field(header, 0, TAR_NAME_LENGTH)
        val prefix = field(header, TAR_PREFIX_OFFSET, TAR_PREFIX_LENGTH)
        val size = field(header, TAR_SIZE_OFFSET, TAR_SIZE_LENGTH).trim().toLong(OCTAL)
        val path = if (prefix.isEmpty()) name else "$prefix/$name"
        val type = header[TAR_TYPE_OFFSET]
        val regularFile = type == '0'.code.toByte() || type == 0.toByte()
        if (regularFile && NEEDED_ENTRY.matches(path)) {
          entries[path.removePrefix(PACKAGE_DIRECTORY)] =
              stream.readNBytes(size.toInt()).toString(Charsets.UTF_8)
        } else {
          stream.skipNBytes(size)
        }
        stream.skipNBytes((TAR_BLOCK - size % TAR_BLOCK) % TAR_BLOCK)
      }
    }
    return entries
  }

  /** Each dictionary of the package's index in rank order, one word per line. */
  private fun writeDictionaries(
      tarballName: String,
      entries: Map<String, String>,
      directory: File
  ) {
    val source = entries.getValue("src/index.ts")
    val imports =
        Regex("import (\\w+) from '\\./(\\w+)\\.json'").findAll(source).associate {
          it.groupValues[1] to it.groupValues[2]
        }
    val body = source.substringAfter("const dictionary = {").substringBefore("}")
    val named =
        Regex("'([^']+)': (\\w+)").findAll(body).map {
          it.groupValues[1] to readList(entries, imports.getValue(it.groupValues[2]))
        }
    val grouped =
        Regex("\\.\\.\\.(\\w+)").findAll(body).flatMap {
          readGroups(entries, imports.getValue(it.groupValues[1])).entries.map { (name, words) ->
            name to words
          }
        }
    val dictionaries = (named + grouped).toList()
    dictionaries.forEach { (name, _) -> checkedName(tarballName, name) }
    directory.mkdirs()
    val index =
        dictionaries.map { (name, words) ->
          if (!words.all { it.isNotEmpty() && it == it.trim() && '\n' !in it }) {
            throw GradleException("$name contains a word which does not fit on one line")
          }
          File(directory, "$name.txt").writeText(words.joinToString("") { "$it\n" })
          "$name $name.txt\n"
        }
    File(directory, "index.txt").writeText(index.joinToString(""))
  }

  @Suppress("UNCHECKED_CAST")
  private fun readList(entries: Map<String, String>, file: String) =
      JsonSlurper().parseText(entries.getValue("src/$file.json")) as List<String>

  @Suppress("UNCHECKED_CAST")
  private fun readGroups(entries: Map<String, String>, file: String) =
      JsonSlurper().parseText(entries.getValue("src/$file.json")) as Map<String, List<String>>

  /** A name from the package's content, which must not be able to leave the output directory. */
  private fun checkedName(tarballName: String, name: String): String {
    if (!SAFE_NAME.matches(name)) {
      throw GradleException("Unexpected name in $tarballName: $name")
    }
    return name
  }

  private fun field(header: ByteArray, offset: Int, length: Int) =
      String(header, offset, length, Charsets.UTF_8).substringBefore('\u0000')
}
