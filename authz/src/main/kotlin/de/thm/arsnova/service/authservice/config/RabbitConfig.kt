package de.thm.arsnova.service.authservice.config

import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory

@Configuration
@EnableConfigurationProperties(AuthServiceProperties::class)
class RabbitConfig (
        private var authServiceProperties: AuthServiceProperties
) : RabbitListenerConfigurer {
    @Bean
    @Autowired
    fun connectionFactory(
            @TaskExecutorConfig.RabbitConnectionExecutor executor: TaskExecutor?
    ): ConnectionFactory? {
        val connectionFactory = CachingConnectionFactory(
                authServiceProperties.rabbitmq?.host,
                authServiceProperties.rabbitmq?.port!!
        )
        connectionFactory.username = authServiceProperties.rabbitmq?.username.orEmpty()
        connectionFactory.setPassword(authServiceProperties.rabbitmq?.password.orEmpty())
        connectionFactory.virtualHost = authServiceProperties.rabbitmq?.virtualHost.orEmpty()
        connectionFactory.setExecutor(executor!!)
        return connectionFactory
    }

    @Bean
    @Autowired
    fun rabbitTemplate(connectionFactory: ConnectionFactory?, messageConverter: MessageConverter?): RabbitTemplate? {
        val rabbitTemplate = RabbitTemplate(connectionFactory!!)
        rabbitTemplate.messageConverter = messageConverter!!
        return rabbitTemplate
    }

    @Bean
    @Autowired
    fun rabbitAdmin(connectionFactory: ConnectionFactory?): RabbitAdmin? {
        return RabbitAdmin(connectionFactory!!)
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter? {
        return Jackson2JsonMessageConverter()
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
