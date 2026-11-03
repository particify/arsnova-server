/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import com.nulabinc.zxcvbn.StandardDictionaries
import com.nulabinc.zxcvbn.StandardKeyboards
import com.nulabinc.zxcvbn.Zxcvbn
import com.nulabinc.zxcvbn.ZxcvbnBuilder
import com.nulabinc.zxcvbn.io.ClasspathResource
import com.nulabinc.zxcvbn.matchers.Dictionary
import com.nulabinc.zxcvbn.matchers.SlantedKeyboardLoader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Layouts besides zxcvbn's own, as used in the languages the dictionaries cover. */
private val ADDITIONAL_KEYBOARDS = listOf("azerty", "qwertz")

/**
 * Holds a directory of dictionaries per language, named by its ISO 639-1 code, plus one with those
 * which belong to no language.
 */
private const val ADDITIONAL_DICTIONARY_DIRECTORY = "password/dictionaries"
private const val LANGUAGE_INDEPENDENT_DIRECTORY = "common"
private const val LANGUAGE_LIST = "languages.txt"

/** Each line names a dictionary followed by the files holding its words in rank order. */
private const val DICTIONARY_INDEX = "index.txt"

/** Keeps the dictionaries of zxcvbn-ts apart from zxcvbn's own, some of which share a topic. */
internal const val ADDITIONAL_DICTIONARY_PREFIX = "zxcvbn-ts-"

private fun keyboardResource(name: String) = "/password/keyboards/$name.txt"

/** The languages the build has generated dictionaries for from the zxcvbn-ts packages. */
internal val availableDictionaryLanguages: List<String> by lazy { readLines(LANGUAGE_LIST) }

/**
 * Its dictionaries take tens of megabytes, so an estimator is shared by every application context
 * in the JVM which configures the same languages, such as those cached by the tests. Estimates
 * leave it unchanged.
 */
private val estimators = ConcurrentHashMap<Set<String>, Zxcvbn>()

/**
 * The estimator built from zxcvbn's own dictionaries and keyboards plus the word lists of
 * zxcvbn-ts, which the build generates from the npm packages pinned in their own version catalog.
 * zxcvbn's own dictionaries are English only.
 */
internal fun passwordEstimator(languages: Collection<String>): Zxcvbn =
    estimators.computeIfAbsent(checkLanguages(languages), ::buildPasswordEstimator)

/** Same as [passwordEstimator], but built anew, so that it can be collected once unused. */
internal fun unsharedPasswordEstimator(languages: Collection<String>): Zxcvbn =
    buildPasswordEstimator(checkLanguages(languages))

private fun checkLanguages(languages: Collection<String>): Set<String> {
  val unknown = languages - availableDictionaryLanguages.toSet()
  require(unknown.isEmpty()) {
    "No password dictionaries for ${unknown.joinToString(", ")} in " +
        "security.password.dictionary-languages. Available languages: " +
        availableDictionaryLanguages.joinToString(", ")
  }
  return languages.toSortedSet()
}

private fun buildPasswordEstimator(languages: Set<String>): Zxcvbn {
  val builder =
      ZxcvbnBuilder()
          .dictionaries(StandardDictionaries.loadAllDictionaries())
          .keyboards(StandardKeyboards.loadAllKeyboards())
          .dictionaries(loadDictionaries(LANGUAGE_INDEPENDENT_DIRECTORY))
  languages.forEach { builder.dictionaries(loadDictionaries(it)) }
  ADDITIONAL_KEYBOARDS.forEach {
    builder.keyboard(SlantedKeyboardLoader(it, ClasspathResource(keyboardResource(it))).load())
  }
  return builder.build()
}

/** The estimate compares lowercase only, which a few lists with capitals would otherwise miss. */
internal fun loadDictionaries(directory: String): List<Dictionary> =
    readLines("$directory/$DICTIONARY_INDEX").map { entry ->
      val (name, files) = entry.split(" ").let { it.first() to it.drop(1) }
      val words = files.flatMap { readLines("$directory/$it") }.map { it.lowercase(Locale.ROOT) }
      Dictionary(ADDITIONAL_DICTIONARY_PREFIX + name, words)
    }

private fun readLines(file: String): List<String> =
    ClasspathResource("/$ADDITIONAL_DICTIONARY_DIRECTORY/$file").inputStream.use {
      it.bufferedReader(Charsets.UTF_8).readLines()
    }
