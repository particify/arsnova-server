/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import java.util.Locale

/**
 * ISO 639 language codes in ascending order, without the obsolete ones (`iw`, `in`, `ji`) which
 * `Locale` silently replaces by their successors.
 */
val LANGUAGE_ISO_639_CODES: Set<String> =
    Locale.getISOLanguages().filter { it == Locale.of(it).language }.toSortedSet()
