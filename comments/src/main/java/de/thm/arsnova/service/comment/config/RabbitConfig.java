package de.thm.arsnova.service.comment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

@Configuration
public class RabbitConfig implements RabbitListenerConfigurer {
    public static final String BACKEND_COMMENT_FANOUT_NAME = "backend.event.comment.beforecreation";
    public static final String BACKEND_COMMENT_QUEUE_NAME = BACKEND_COMMENT_FANOUT_NAME + ".consumer.comment-service";
    public static final String COMMENT_SERVICE_COMMENT_DELETE_FANOUT_NAME = "commentservice.event.comment.deleted";

    @Value("${spring.rabbitmq.host}") private String rabbitmqHost;
    @Value("${spring.rabbitmq.port}") private int rabbitmqPort;
    @Value("${spring.rabbitmq.username}") private String rabbitmqUsername;
    @Value("${spring.rabbitmq.password}") private String rabbitmqPassword;
    @Value("${spring.rabbitmq.vhost}") private String rabbitmqVhost;

    @Bean
    @Autowired
    public ConnectionFactory connectionFactory(
            @TaskExecutorConfig.RabbitConnectionExecutor TaskExecutor executor
    ) {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitmqHost, rabbitmqPort);
        connectionFactory.setUsername(rabbitmqUsername);
        connectionFactory.setPassword(rabbitmqPassword);
        connectionFactory.setVirtualHost(rabbitmqVhost);
        connectionFactory.setExecutor(executor);
        return connectionFactory;
    }

    @Bean
    @Autowired
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MappingJackson2MessageConverter jackson2Converter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        return converter;
    }

    @Bean
    public DefaultMessageHandlerMethodFactory myHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setMessageConverter(jackson2Converter());
        return factory;
    }

    @Bean
    @Autowired
    public Declarables rabbitDeclarables() {
        final FanoutExchange fanoutExchange = new FanoutExchange(BACKEND_COMMENT_FANOUT_NAME);
        final Queue queue = new Queue(BACKEND_COMMENT_QUEUE_NAME, true, false, false);
        final Binding binding = BindingBuilder.bind(queue).to(fanoutExchange);

        final FanoutExchange deleteFanoutExchange = new FanoutExchange(COMMENT_SERVICE_COMMENT_DELETE_FANOUT_NAME);

        return new Declarables(fanoutExchange, queue, binding, deleteFanoutExchange);
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(myHandlerMethodFactory());
    }
}
