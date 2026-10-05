/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import com.nimbusds.jwt.SignedJWT
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Account from the `dev` Liquibase fixtures which owns [OWNED_ROOM_ID]. */
private val OWNER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val OWNED_ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** The internal token the gateway requests for a room, as the user's role in that room. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class JwtControllerHttpTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var jwtUtils: JwtUtils
  @Autowired lateinit var userService: UserService

  @Test
  fun shouldIssueTokenWithRoomRoleForMember() {
    val response = requestInternalToken(OWNER_ID).andExpect(status().isOk()).andReturn().response
    val token = checkNotNull(response.getHeader(HttpHeaders.AUTHORIZATION)).removePrefix("Bearer ")
    val claims = SignedJWT.parse(token).jwtClaimsSet
    Assertions.assertEquals(jwtUtils.uuidToString(OWNER_ID), claims.subject)
    Assertions.assertEquals(
        listOf("OWNER-${jwtUtils.uuidToString(OWNED_ROOM_ID)}"), claims.getStringListClaim("roles"))
  }

  @Test
  fun shouldRejectNonMember() {
    val nonMemberId = checkNotNull(userService.createAccount().id)
    requestInternalToken(nonMemberId).andExpect(status().isForbidden())
  }

  private fun requestInternalToken(userId: UUID): ResultActions {
    val publicToken = jwtUtils.encodeJwt(userId.toString(), listOf())
    return mockMvc.perform(
        get("/jwt")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $publicToken")
            .header("X-Forwarded-Uri", "/api/room/$OWNED_ROOM_ID/contents"))
  }
}
