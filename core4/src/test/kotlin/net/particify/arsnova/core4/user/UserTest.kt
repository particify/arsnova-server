/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UserTest {
  @Test
  fun shouldConvertMailAddressesToLowerCase() {
    val mailAddress = "shouldConvertToLowerCase@eXample.com"
    val lowerCaseMailAddress = "shouldconverttolowercase@example.com"
    val user = User(mailAddress = mailAddress, unverifiedMailAddress = mailAddress)
    Assertions.assertEquals(lowerCaseMailAddress, user.mailAddress)
    Assertions.assertEquals(lowerCaseMailAddress, user.unverifiedMailAddress)
    user.mailAddress = mailAddress
    Assertions.assertEquals(lowerCaseMailAddress, user.mailAddress)
    user.unverifiedMailAddress
    Assertions.assertEquals(lowerCaseMailAddress, user.unverifiedMailAddress)
  }

  /** The v3 migration parks a mail address without initiating a verification for it. */
  @Test
  fun shouldNotConsiderVerificationActiveWithoutExpiry() {
    val parked = User(unverifiedMailAddress = "parked@example.com")
    Assertions.assertFalse(parked.isMailAddressVerificationActive())
    Assertions.assertFalse(User().isPasswordResetVerificationActive())
  }
}
