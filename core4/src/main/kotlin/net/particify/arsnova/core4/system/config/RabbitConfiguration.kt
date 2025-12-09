/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfiguration {
  @Bean
  fun rabbitContainer(
      connectionFactory: ConnectionFactory,
  ): SimpleMessageListenerContainer {
    val container = SimpleMessageListenerContainer()
    container.connectionFactory = connectionFactory
    return container
  }

  @Bean
  fun rabbitTemplate(
      connectionFactory: ConnectionFactory,
      messageConverter: MessageConverter
  ): RabbitTemplate {
    val rabbitTemplate = RabbitTemplate(connectionFactory)
    rabbitTemplate.messageConverter = messageConverter
    return rabbitTemplate
  }

  @Bean
  fun messageConverter(): JacksonJsonMessageConverter {
    return JacksonJsonMessageConverter()
  }
}
