/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException

class SlideContentFormatData : ContentFormatData {
  override val format = "SLIDE"

  override fun answerable() = false

  /** An update of another format would reinterpret the content's rounds and answers. */
  override fun validateChange(current: ContentFormatData, hasAnswers: () -> Boolean) {
    if (current !is SlideContentFormatData) {
      throw ContentFormatDataInvalidException("Content is not a slide content")
    }
  }
}
