package net.particify.arsnova.gateway.controller

import net.particify.arsnova.gateway.model.Membership
import net.particify.arsnova.gateway.view.MembershipView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux

@Controller
class MembershipController(
  private val membershipView: MembershipView
) {

  companion object {
    const val baseMapping = "/_view/membership"
    const val byUserMapping = "$baseMapping/by-user/{userId}"
    const val byUserAndRoomMapping = "$baseMapping/by-user-and-room/{userId}/{roomId}"
  }

  private val logger = LoggerFactory.getLogger(MembershipController::class.java)

  @GetMapping(path = [byUserMapping])
  @ResponseBody
  fun getMembershipByUser(
    @PathVariable userId: String
  ): Flux<Membership> {
    logger.trace("Getting membership by user: {}", userId)
    return membershipView.getByUser(userId)
  }
}
