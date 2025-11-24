package net.particify.arsnova.authz.config

import net.particify.arsnova.authz.model.event.RoomAccessSyncEvent
import net.particify.arsnova.authz.model.event.RoomAccessSyncRequest
import org.springframework.amqp.core.BindingBuilder.bind
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.ClassMapper
import org.springframework.amqp.support.converter.DefaultClassMapper
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableConfigurationProperties(AuthServiceProperties::class)
class RabbitConfig(
  private var authServiceProperties: AuthServiceProperties,
) {
  companion object {
    const val ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME: String = "backend.event.room.access.sync.response"
    const val ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME: String = "backend.event.room.access.sync.request"
    const val ROOM_CREATED_EXCHANGE_NAME: String = "backend.event.room.aftercreation"
    const val ROOM_DELETED_EXCHANGE_NAME: String = "backend.event.room.afterdeletion"
    const val ROOM_CREATED_QUEUE_NAME: String = "backend.event.room.aftercreation.consumer.auth-service"
    const val ROOM_DELETED_QUEUE_NAME: String = "backend.event.room.afterdeletion.consumer.auth-service"
    const val PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME: String = "backend.event.migration.access.participant"
  }

  @Bean
  fun connectionFactory(
    @TaskExecutorConfig.RabbitConnectionExecutor executor: TaskExecutor,
  ): ConnectionFactory {
    val connectionFactory =
      CachingConnectionFactory(
        authServiceProperties.rabbitmq.host,
        authServiceProperties.rabbitmq.port,
      )
    connectionFactory.username = authServiceProperties.rabbitmq.username
    connectionFactory.setPassword(authServiceProperties.rabbitmq.password)
    connectionFactory.virtualHost = authServiceProperties.rabbitmq.virtualHost
    connectionFactory.setExecutor(executor)
    return connectionFactory
  }

  @Bean
  fun rabbitTemplate(
    connectionFactory: ConnectionFactory,
    messsageConverter: MessageConverter,
  ): RabbitTemplate {
    val rabbitTemplate = RabbitTemplate(connectionFactory)
    rabbitTemplate.messageConverter = messsageConverter
    return rabbitTemplate
  }

  @Bean
  fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin = RabbitAdmin(connectionFactory)

  @Bean
  fun declarables(): Declarables {
    val roomCreatedFanoutExchange = FanoutExchange(ROOM_CREATED_EXCHANGE_NAME)
    val roomDeletedFanoutExchange = FanoutExchange(ROOM_DELETED_EXCHANGE_NAME)
    val participantAccessMigrationDlq = Queue("$PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME.dlq")
    val participantAccessMigrationQueue =
      Queue(
        PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME,
        true,
        false,
        false,
        mapOf(
          "x-dead-letter-exchange" to "",
          "x-dead-letter-routing-key" to "$PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME.dlq",
        ),
      )
    val roomCreatedQueue =
      Queue(
        ROOM_CREATED_QUEUE_NAME,
        true,
        true,
        false,
        mapOf(
          "x-dead-letter-exchange" to "",
          "x-dead-letter-routing-key" to "$ROOM_CREATED_QUEUE_NAME.dlq",
        ),
      )
    val roomDeletedQueue =
      Queue(
        ROOM_DELETED_QUEUE_NAME,
        true,
        true,
        false,
        mapOf(
          "x-dead-letter-exchange" to "",
          "x-dead-letter-routing-key" to "$ROOM_DELETED_QUEUE_NAME.dlq",
        ),
      )
    val roomCreatedDlq = Queue("$ROOM_CREATED_QUEUE_NAME.dlq")
    val roomDeletedDlq = Queue("$ROOM_DELETED_QUEUE_NAME.dlq")
    val roomAccessSyncResponseQueue =
      Queue(
        ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME,
        true,
        false,
        false,
        mapOf(
          "x-dead-letter-exchange" to "",
          "x-dead-letter-routing-key" to "$ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME.dlq",
        ),
      )
    val roomAccessSyncResponseDlq = Queue("$ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME.dlq")
    val roomAccessSyncRequestExchange = DirectExchange(ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME)

    return Declarables(
      listOf(
        roomCreatedFanoutExchange,
        roomDeletedFanoutExchange,
        participantAccessMigrationQueue,
        participantAccessMigrationDlq,
        roomCreatedQueue,
        roomCreatedDlq,
        roomDeletedQueue,
        roomDeletedDlq,
        bind(roomCreatedQueue).to(roomCreatedFanoutExchange),
        bind(roomDeletedQueue).to(roomDeletedFanoutExchange),
        roomAccessSyncResponseQueue,
        roomAccessSyncResponseDlq,
        roomAccessSyncRequestExchange,
      ),
    )
  }

  @Bean
  fun jsonMessageConverter(jsonMapper: JsonMapper): MessageConverter = JacksonJsonMessageConverter(jsonMapper)

  @Bean
  fun classMapper(): ClassMapper {
    val classMapper = DefaultClassMapper()
    val idClassMapping: MutableMap<String, Class<*>> = HashMap()
    // Because of not having shared models we need this mapping to backend models for jackson
    idClassMapping["net.particify.arsnova.core.event.RoomAccessSyncRequest"] = RoomAccessSyncRequest::class.java
    idClassMapping["net.particify.arsnova.core.event.RoomAccessSyncEvent"] = RoomAccessSyncEvent::class.java
    classMapper.setIdClassMapping(idClassMapping)
    return classMapper
  }
}
