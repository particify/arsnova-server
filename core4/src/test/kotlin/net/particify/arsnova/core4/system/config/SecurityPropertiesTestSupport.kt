/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import java.time.Duration
import net.particify.arsnova.core4.system.config.SecurityProperties.RoomCreatorRole.AutoAssignment

private const val SECRET = "weaksecret1234567890123456789012"
private const val BCRYPT_STRENGTH = 12
private const val CHALLENGE_VALIDITY_SECONDS = 60L
private const val CHALLENGE_ITERATIONS = 5000
private const val LOGIN_ATTEMPT_LIMIT = 20L
private const val EXTERNAL_SESSION_DAYS = 30L
private const val REMEMBER_ME_DAYS = 180L

/** Everything a test does not pass carries the value shipped in `application.yaml`. */
fun securityProperties(
    localAccount: SecurityProperties.LocalAccount = localAccount(),
    login: SecurityProperties.Login = login(),
    roomCreatorRole: SecurityProperties.RoomCreatorRole =
        SecurityProperties.RoomCreatorRole(AutoAssignment.VERIFIED_ACCOUNTS)
) =
    SecurityProperties(
        password = SecurityProperties.Password(BCRYPT_STRENGTH),
        jwt = SecurityProperties.Jwt(SECRET, "issuer", null, Duration.ofHours(1)),
        challenge =
            SecurityProperties.Challenge(
                CHALLENGE_VALIDITY_SECONDS,
                "PBKDF2/SHA-256",
                SECRET,
                CHALLENGE_ITERATIONS,
                CHALLENGE_ITERATIONS),
        login = login,
        localAccount = localAccount,
        roomCreatorRole = roomCreatorRole,
        authorizeUriHeader = "X-Forwarded-Uri",
        authorizeUriPrefix = "/api")

fun login(
    externalSessionMaxAge: Duration? = Duration.ofDays(EXTERNAL_SESSION_DAYS),
    rememberMeMaxAge: Duration? = Duration.ofDays(REMEMBER_ME_DAYS)
) =
    SecurityProperties.Login(
        attemptLimit = LOGIN_ATTEMPT_LIMIT,
        attemptWindow = Duration.ofMinutes(2),
        externalSessionMaxAge = externalSessionMaxAge,
        rememberMeMaxAge = rememberMeMaxAge)

fun localAccount(
    enabled: Boolean = true,
    selfRegistrationEnabled: Boolean = true,
    allowedMailAddressDomains: List<String> = listOf()
) = SecurityProperties.LocalAccount(enabled, selfRegistrationEnabled, allowedMailAddressDomains)
