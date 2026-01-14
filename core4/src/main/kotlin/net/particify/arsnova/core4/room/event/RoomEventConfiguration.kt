/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import org.springframework.amqp.core.Exchange
import org.springframework.amqp.core.ExchangeBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RoomEventConfiguration {
  @Bean
  fun roomCreatedExchange(): Exchange {
    return ExchangeBuilder.fanoutExchange(ROOM_CREATED_DESTINATION).build()
  }

  @Bean
  fun roomDeletedExchange(): Exchange {
    return ExchangeBuilder.fanoutExchange(ROOM_DELETED_DESTINATION).build()
  }

  @Bean
  fun roomDuplicatedExchange(): Exchange {
    return ExchangeBuilder.fanoutExchange(ROOM_DUPLICATED_DESTINATION).build()
  }
}
