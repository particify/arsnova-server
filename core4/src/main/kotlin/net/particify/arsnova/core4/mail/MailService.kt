/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.mail

import java.util.Locale

interface MailService {
  fun sendMail(address: String, template: String, data: Map<String, Any>, locale: Locale)
}
