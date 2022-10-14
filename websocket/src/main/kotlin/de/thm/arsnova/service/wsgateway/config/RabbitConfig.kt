package de.thm.arsnova.service.wsgateway.config

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor

@Configuration
@EnableConfigurationProperties(WebSocketProperties::class)
class RabbitConfig(
  private val webSocketProperties: WebSocketProperties
) {
  @Bean
  @Autowired
  fun connectionFactory(
    @TaskExecutorConfig.RabbitConnectionExecutor executor: TaskExecutor?
  ): ConnectionFactory? {
    val connectionFactory = CachingConnectionFactory(
      webSocketProperties.rabbitmq.host,
      webSocketProperties.rabbitmq.port
    )
    connectionFactory.username = webSocketProperties.rabbitmq.username
    connectionFactory.setPassword(webSocketProperties.rabbitmq.password)
    connectionFactory.virtualHost = webSocketProperties.rabbitmq.virtualHost
    connectionFactory.setExecutor(executor!!)
    return connectionFactory
  }

  @Bean
  @Autowired
  fun rabbitTemplate(connectionFactory: ConnectionFactory?): RabbitTemplate? {
    val rabbitTemplate = RabbitTemplate(connectionFactory!!)
    rabbitTemplate.messageConverter = jsonMessageConverter()
    return rabbitTemplate
  }

  @Bean
  fun jsonMessageConverter(): MessageConverter {
    val converter = Jackson2JsonMessageConverter()
    return converter
  }
}
