/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import net.particify.arsnova.core4.user.internal.UsernameMapping
import net.particify.arsnova.core4.user.internal.resolveUsername
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UsernameMappingTests {
  @Test
  fun shouldLowercaseMappedId() {
    Assertions.assertEquals(
        "mixedcase-subject-id",
        resolveUsername(UsernameMapping.ID, "MixedCase-Subject-Id", "user@example.com"))
  }

  @Test
  fun shouldLowercaseMappedMailAddress() {
    Assertions.assertEquals(
        "user@example.com",
        resolveUsername(UsernameMapping.MAIL_ADDRESS, "subject-id", "User@Example.com"))
  }

  @Test
  fun shouldResolveIdWithoutMailAddress() {
    Assertions.assertEquals("subject-id", resolveUsername(UsernameMapping.ID, "subject-id", null))
  }

  /** Not every identity provider releases the mail attribute. */
  @Test
  fun shouldNotResolveMissingMailAddress() {
    Assertions.assertNull(resolveUsername(UsernameMapping.MAIL_ADDRESS, "subject-id", null))
  }
}
