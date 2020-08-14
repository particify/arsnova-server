package de.thm.arsnova.service.authservice.config

import de.thm.arsnova.service.authservice.model.event.RoomAccessSyncEvent
import de.thm.arsnova.service.authservice.model.event.RoomAccessSyncRequest
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar
import org.springframework.amqp.support.converter.ClassMapper
import org.springframework.amqp.support.converter.DefaultClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory
import java.util.HashMap

@Configuration
@EnableConfigurationProperties(AuthServiceProperties::class)
class RabbitConfig (
        private var authServiceProperties: AuthServiceProperties
) : RabbitListenerConfigurer {

    companion object {
        const val roomAccessGrantedQueueName: String = "backend.event.room.access.granted"
        const val roomAccessRevokedQueueName: String = "backend.event.room.access.revoked"
        const val roomAccessSyncResponseQueueName: String = "backend.event.room.access.sync.response"
        const val roomAccessSyncRequestQueueName: String = "backend.event.room.access.sync.request"
    }

    @Bean
    @Autowired
    fun connectionFactory(
            @TaskExecutorConfig.RabbitConnectionExecutor executor: TaskExecutor?
    ): ConnectionFactory? {
        val connectionFactory = CachingConnectionFactory(
                authServiceProperties.rabbitmq.host,
                authServiceProperties.rabbitmq.port
        )
        connectionFactory.username = authServiceProperties.rabbitmq.username
        connectionFactory.setPassword(authServiceProperties.rabbitmq.password)
        connectionFactory.virtualHost = authServiceProperties.rabbitmq.virtualHost
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
    @Autowired
    fun rabbitAdmin(connectionFactory: ConnectionFactory?): RabbitAdmin? {
        return RabbitAdmin(connectionFactory!!)
    }

    @Bean
    fun declarables(): Declarables {
        return Declarables(listOf(
                Queue(roomAccessGrantedQueueName, true),
                Queue(roomAccessRevokedQueueName, true),
                Queue(roomAccessSyncResponseQueueName, true),
                DirectExchange(roomAccessSyncRequestQueueName)
        ))
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        val converter = Jackson2JsonMessageConverter()
        return converter
    }

    @Bean
    fun classMapper(): ClassMapper {
        val classMapper = DefaultClassMapper()
        val idClassMapping: MutableMap<String, Class<*>> = HashMap()
        // Because of not having shared models we need this mapping to backend models for jackson
        idClassMapping["de.thm.arsnova.event.RoomAccessSyncRequest"] = RoomAccessSyncRequest::class.java
        idClassMapping["de.thm.arsnova.event.RoomAccessSyncEvent"] = RoomAccessSyncEvent::class.java
        classMapper.setIdClassMapping(idClassMapping)
        return classMapper
    }

    @Bean
    fun jackson2Converter(): MappingJackson2MessageConverter? {
        return MappingJackson2MessageConverter()
    }

    @Bean
    fun myHandlerMethodFactory(): DefaultMessageHandlerMethodFactory? {
        val factory = DefaultMessageHandlerMethodFactory()
        factory.setMessageConverter(jackson2Converter()!!)
        return factory
    }

    override fun configureRabbitListeners(registrar: RabbitListenerEndpointRegistrar) {
        registrar.messageHandlerMethodFactory = myHandlerMethodFactory()!!
    }
}
