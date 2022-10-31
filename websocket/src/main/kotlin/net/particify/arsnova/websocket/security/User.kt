package net.particify.arsnova.websocket.security

import org.springframework.core.style.ToStringCreator
import java.security.Principal

class User(private val userId: String) : Principal {
  fun getUserId(): String {
    return userId
  }

  override fun getName(): String {
    return userId
  }

  override fun toString(): String {
    return ToStringCreator(this)
      .append("userId", userId)
      .toString()
  }
}
