package net.particify.arsnova.websocket.security

import org.springframework.core.style.ToStringCreator
import java.security.Principal

class User(
  val userId: String,
  val jwt: String,
) : Principal {
  override fun getName(): String = userId

  override fun toString(): String =
    ToStringCreator(this)
      .append("userId", userId)
      .toString()
}
