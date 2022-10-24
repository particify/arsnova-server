package de.thm.arsnova.service.wsgateway.controller

import de.thm.arsnova.service.wsgateway.service.RoomSubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("RoomSubscriptionController")
class RoomSubscriptionController(
  private val roomSubscriptionService: RoomSubscriptionService
) {
  companion object {
    const val ROOM_SUBSCRIPTION_MAPPING = "/roomsubscription"
    const val GET_USER_COUNT_SUBSCRIPTION = "$ROOM_SUBSCRIPTION_MAPPING/usercount"
  }

  private val logger = LoggerFactory.getLogger(RoomSubscriptionController::class.java)

  @GetMapping(GET_USER_COUNT_SUBSCRIPTION)
  fun getUserCount(
    @RequestParam ids: List<String>
  ): List<Int?> {
    return ids.map { id -> roomSubscriptionService.getUserCount(id) }
  }
}
