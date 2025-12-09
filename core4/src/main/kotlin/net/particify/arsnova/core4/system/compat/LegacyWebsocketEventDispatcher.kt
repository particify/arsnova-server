/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import net.particify.arsnova.core4.common.EntityChangeEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class LegacyWebsocketEventDispatcher(val rabbitTemplate: RabbitTemplate) {
  @EventListener
  fun handleEntityChangeEvent(event: EntityChangeEvent<Any>) {
    val idString = event.entityId.toString().replace("-", "")
    val wsEvent =
        WebsocketEntityChangeEvent(
            event.entityType.simpleName!!, event.changeType.toString(), idString)
    rabbitTemplate.convertAndSend("amq.topic", "$idString.changes-meta.stream", wsEvent)
  }

  data class WebsocketEntityChangeEvent(
      val entityType: String,
      val changeType: String,
      val entityId: String
  )
}
