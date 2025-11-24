package net.particify.arsnova.core.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

import net.particify.arsnova.core.config.properties.MessageBrokerProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.event.AmqpMigrationEventDispatcher;
import net.particify.arsnova.core.event.OutgoingAmqpEventDispatcher;
import net.particify.arsnova.core.event.RoomAccessEventDispatcher;
import net.particify.arsnova.core.websocket.handler.FeedbackHandler;

@Configuration
@EnableRabbit
@ComponentScan(basePackages = "net.particify.arsnova.core.websocket.handler")
@EnableConfigurationProperties(MessageBrokerProperties.class)
public class RabbitConfig {
  public static final String ROOM_BEFORE_DELETION_EXCHANGE_NAME = "backend.event.room.beforedeletion";
  public static final String ROOM_BEFORE_DELETION_QUEUE_NAME = "backend.event.room.beforedeletion.consumer.core";
  public static final String ROOM_AFTER_DELETION_EXCHANGE_NAME = "backend.event.room.afterdeletion";
  public static final String ROOM_AFTER_DELETION_QUEUE_NAME = "backend.event.room.afterdeletion.consumer.core";
  public static final String ROOM_DUPLICATION_EXCHANGE_NAME = "backend.event.room.duplicated";
  public static final String ROOM_DUPLICATION_QUEUE_NAME = "backend.event.room.duplicated.consumer.core";

  private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

  public static class RabbitConfigProperties {
    public static final String RABBIT_ENABLED = "rabbitmq.enabled";
    private static final String RABBIT_MANAGE_DECLARATIONS = "rabbitmq.manage-declarations";
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public ConnectionFactory connectionFactory(
      @TaskExecutorConfig.RabbitConnectionExecutor final TaskExecutor executor,
      final MessageBrokerProperties messageBrokerProperties
  ) {
    final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
        messageBrokerProperties.getRabbitmq().getHost(),
        messageBrokerProperties.getRabbitmq().getPort());
    connectionFactory.setUsername(messageBrokerProperties.getRabbitmq().getUsername());
    connectionFactory.setPassword(messageBrokerProperties.getRabbitmq().getPassword());
    connectionFactory.setVirtualHost(messageBrokerProperties.getRabbitmq().getVirtualHost());
    connectionFactory.setExecutor(executor);

    return connectionFactory;
  }

  @Bean
  @ConditionalOnProperty(
      name = {RabbitConfigProperties.RABBIT_ENABLED, RabbitConfigProperties.RABBIT_MANAGE_DECLARATIONS},
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public Declarables declarables(
      final MessageBrokerProperties messageBrokerProperties,
      final SystemProperties systemProperties) {
    final Set<String> eventExchanges = messageBrokerProperties.getPublishedEvents().stream()
        .map(c -> OutgoingAmqpEventDispatcher.makeQueueName(c.entityType, c.eventType)).collect(Collectors.toSet());

    final List<Declarable> declarables = eventExchanges.stream()
        .map(FanoutExchange::new).collect(Collectors.toList());

    final FanoutExchange roomDuplicationExchange = new FanoutExchange(ROOM_DUPLICATION_EXCHANGE_NAME);
    declarables.add(roomDuplicationExchange);
    declarables.add(new Queue(
        RoomAccessEventDispatcher.ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME,
        true,
        false,
        false,
        Map.of(
            "x-dead-letter-exchange", "",
            "x-dead-letter-routing-key", RoomAccessEventDispatcher.ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME + ".dlq"
        )
    ));
    declarables.add(new Queue(RoomAccessEventDispatcher.ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME + ".dlq"));
    declarables.add(new Queue(
        RoomAccessEventDispatcher.ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME,
        true,
        false,
        false,
        Map.of(
            "x-dead-letter-exchange", "",
            "x-dead-letter-routing-key", RoomAccessEventDispatcher.ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME + ".dlq"
        )
    ));
    declarables.add(new Queue(RoomAccessEventDispatcher.ROOM_ACCESS_SYNC_RESPONSE_QUEUE_NAME + ".dlq"));
    declarables.add(new Queue(FeedbackHandler.CREATE_FEEDBACK_COMMAND_QUEUE_NAME, true));
    declarables.add(new Queue(FeedbackHandler.CREATE_FEEDBACK_RESET_COMMAND_QUEUE_NAME, true));
    declarables.add(new Queue(FeedbackHandler.QUERY_FEEDBACK_COMMAND_QUEUE_NAME, true));
    declarables.add(new Queue(AmqpMigrationEventDispatcher.PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME,
        true,
        false,
        false,
        Map.of(
            "x-dead-letter-exchange",
            "",
            "x-dead-letter-routing-key",
            AmqpMigrationEventDispatcher.PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME + ".dlq"
        )
    ));
    declarables.add(new Queue(AmqpMigrationEventDispatcher.PARTICIPANT_ACCESS_MIGRATION_QUEUE_NAME + ".dlq"));

    if (systemProperties.isExternalRoomManagement()) {
      // With external room management the event direction for rooms is inverted, so we need to receive them instead of
      // sending them. For this we need to bind queues to the exchanges.

      final Queue roomBeforeDeletionQueue = new Queue(ROOM_BEFORE_DELETION_QUEUE_NAME);
      final FanoutExchange roomBeforeDeletionExchange = new FanoutExchange(ROOM_BEFORE_DELETION_EXCHANGE_NAME);
      declarables.add(roomBeforeDeletionExchange);
      declarables.add(roomBeforeDeletionQueue);
      declarables.add(BindingBuilder.bind(roomBeforeDeletionQueue).to(roomBeforeDeletionExchange));

      final Queue roomAfterDeletionQueue = new Queue(ROOM_AFTER_DELETION_QUEUE_NAME);
      final FanoutExchange roomAfterDeletionExchange = new FanoutExchange(ROOM_AFTER_DELETION_EXCHANGE_NAME);
      declarables.add(roomAfterDeletionExchange);
      declarables.add(roomAfterDeletionQueue);
      declarables.add(BindingBuilder.bind(roomAfterDeletionQueue).to(roomAfterDeletionExchange));

      final Queue roomDuplicationQueue = new Queue(ROOM_DUPLICATION_QUEUE_NAME);
      declarables.add(roomDuplicationQueue);
      declarables.add(BindingBuilder.bind(roomDuplicationQueue).to(roomDuplicationExchange));
    }

    return new Declarables(declarables);
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public RabbitTemplate rabbitTemplate(
      final ConnectionFactory connectionFactory,
      final MessageConverter messageConverter) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    return rabbitTemplate;
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public RabbitAdmin rabbitAdmin(final ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public OutgoingAmqpEventDispatcher eventToTopicPublisher(
      final RabbitTemplate rabbitTemplate,
      final MessageBrokerProperties messageBrokerProperties,
      final SystemProperties systemProperties) {
    return new OutgoingAmqpEventDispatcher(rabbitTemplate, messageBrokerProperties, systemProperties);
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public MappingJackson2MessageConverter mappingJackson2MessageConverter() {
    final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    return converter;
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public DefaultMessageHandlerMethodFactory defaultMessageHandlerMethodFactory() {
    final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
    factory.setMessageConverter(mappingJackson2MessageConverter());
    return factory;
  }

  public StatelessRetryOperationsInterceptor retryInterceptor(
      final int maxRetries
  ) {
    return RetryInterceptorBuilder.stateless()
        .maxRetries(maxRetries)
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build();
  }

  @Bean
  @ConditionalOnProperty(
      name = RabbitConfigProperties.RABBIT_ENABLED,
      prefix = MessageBrokerProperties.PREFIX,
      havingValue = "true")
  public SimpleRabbitListenerContainerFactory myRabbitListenerContainerFactory(
      @TaskExecutorConfig.RabbitConnectionExecutor final TaskExecutor executor,
      final MessageBrokerProperties messageBrokerProperties
  ) {
    final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory(executor, messageBrokerProperties));
    factory.setMessageConverter(jsonMessageConverter());
    factory.setMaxConcurrentConsumers(5);
    factory.setAdviceChain(retryInterceptor(messageBrokerProperties.getRabbitmq().getListener().getMaxAttempts()));

    return factory;
  }
}
