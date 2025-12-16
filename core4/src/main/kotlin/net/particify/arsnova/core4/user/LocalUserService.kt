/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.Locale

interface LocalUserService {
  fun inviteUser(
      inviter: User,
      mailAddress: String,
      template: String,
      data: Map<String, Any> = mapOf(),
      locale: Locale
  ): User

  fun verifyUser(user: User): User
}
