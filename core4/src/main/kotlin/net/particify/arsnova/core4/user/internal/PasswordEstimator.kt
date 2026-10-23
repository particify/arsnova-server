/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import com.nulabinc.zxcvbn.StandardDictionaries
import com.nulabinc.zxcvbn.StandardKeyboards
import com.nulabinc.zxcvbn.Zxcvbn
import com.nulabinc.zxcvbn.ZxcvbnBuilder
import com.nulabinc.zxcvbn.io.ClasspathResource
import com.nulabinc.zxcvbn.matchers.SlantedKeyboardLoader

/** The German and French keyboard layouts, which zxcvbn's own QWERTY one does not cover. */
private val ADDITIONAL_KEYBOARDS = listOf("azerty", "qwertz")

private fun keyboardResource(name: String) = "/password/keyboards/$name.txt"

/**
 * The estimator built from zxcvbn's own dictionaries and keyboards plus [ADDITIONAL_KEYBOARDS]. It
 * is shared by every application context in the JVM, such as those cached by the tests. Estimates
 * leave it unchanged.
 */
internal val passwordEstimator: Zxcvbn by lazy {
  val builder =
      ZxcvbnBuilder()
          .dictionaries(StandardDictionaries.loadAllDictionaries())
          .keyboards(StandardKeyboards.loadAllKeyboards())
  ADDITIONAL_KEYBOARDS.forEach {
    builder.keyboard(SlantedKeyboardLoader(it, ClasspathResource(keyboardResource(it))).load())
  }
  builder.build()
}
