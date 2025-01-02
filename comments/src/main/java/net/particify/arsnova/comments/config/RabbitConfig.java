package net.particify.arsnova.comments.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class RabbitConfig {
  public static final String BACKEND_COMMENT_FANOUT_NAME = "backend.event.comment.beforecreation";
  public static final String BACKEND_COMMENT_QUEUE_NAME = BACKEND_COMMENT_FANOUT_NAME + ".consumer.comment-service";
  public static final String BACKEND_ROOM_DUPLICATED_FANOUT_NAME = "backend.event.room.duplicated";
  public static final String BACKEND_ROOM_DUPLICATED_QUEUE_NAME = "backend.event.room.duplicated.consumer.comment-service";
  public static final String COMMENT_SERVICE_COMMENT_DELETE_FANOUT_NAME = "commentservice.event.comment.deleted";
  public static final String ROOM_CREATED_FANOUT_NAME = "backend.event.room.aftercreation";
  public static final String ROOM_CREATED_QUEUE_NAME = ROOM_CREATED_FANOUT_NAME + ".consumer.comment-service";
  public static final String ROOM_CREATED_DLQ_NAME = ROOM_CREATED_QUEUE_NAME + ".dlq";
  public static final String ROOM_DELETED_FANOUT_NAME = "backend.event.room.afterdeletion";
  public static final String ROOM_DELETED_QUEUE_NAME = ROOM_DELETED_FANOUT_NAME + ".consumer.comment-service";
  public static final String ROOM_DELETED_DLQ_NAME = ROOM_DELETED_QUEUE_NAME + ".dlq";

  @Value("${spring.rabbitmq.host}") private String rabbitmqHost;
  @Value("${spring.rabbitmq.port}") private int rabbitmqPort;
  @Value("${spring.rabbitmq.username}") private String rabbitmqUsername;
  @Value("${spring.rabbitmq.password}") private String rabbitmqPassword;
  @Value("${spring.rabbitmq.vhost}") private String rabbitmqVhost;

  @Bean
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
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    return rabbitTemplate;
  }

  @Bean
  public MessageConverter jsonMessageConverter(final ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  public Declarables rabbitDeclarables() {
    final FanoutExchange commentFanoutExchange = new FanoutExchange(BACKEND_COMMENT_FANOUT_NAME);
    final Queue commentQueue = new Queue(BACKEND_COMMENT_QUEUE_NAME, true, false, false);
    final Binding commentBinding = BindingBuilder.bind(commentQueue).to(commentFanoutExchange);
    final FanoutExchange roomDuplicatedFanoutExchange =  new FanoutExchange(BACKEND_ROOM_DUPLICATED_FANOUT_NAME);
    final Queue roomDuplicatedQueue =  new Queue(BACKEND_ROOM_DUPLICATED_QUEUE_NAME, true, false, false);
    final Binding roomDuplicatedBinding = BindingBuilder.bind(roomDuplicatedQueue).to(roomDuplicatedFanoutExchange);

    final FanoutExchange roomCreatedFanoutExchange = new FanoutExchange(ROOM_CREATED_FANOUT_NAME);
    final Queue roomCreatedDlq = new Queue(ROOM_CREATED_DLQ_NAME, true, false, false);
    final Queue roomCreatedQueue = QueueBuilder
      .durable(ROOM_CREATED_QUEUE_NAME)
      .deadLetterExchange("")
      .deadLetterRoutingKey(ROOM_CREATED_DLQ_NAME)
      .build();
    final Binding roomCreatedBinding = BindingBuilder.bind(roomCreatedQueue).to(roomCreatedFanoutExchange);

    final FanoutExchange roomDeletedFanoutExchange = new FanoutExchange(ROOM_DELETED_FANOUT_NAME);
    final Queue roomDeletedDlq = new Queue(ROOM_DELETED_DLQ_NAME, true, false, false);
    final Queue roomDeletedQueue = QueueBuilder
        .durable(ROOM_DELETED_QUEUE_NAME)
        .deadLetterExchange("")
        .deadLetterRoutingKey(ROOM_DELETED_DLQ_NAME)
        .build();
    final Binding roomDeletedBinding = BindingBuilder.bind(roomDeletedQueue).to(roomDeletedFanoutExchange);

    final FanoutExchange deleteFanoutExchange = new FanoutExchange(COMMENT_SERVICE_COMMENT_DELETE_FANOUT_NAME);

    return new Declarables(
        commentFanoutExchange,
        commentQueue,
        commentBinding,
        deleteFanoutExchange,
        roomCreatedFanoutExchange,
        roomCreatedDlq,
        roomCreatedQueue,
        roomCreatedBinding,
        roomDeletedFanoutExchange,
        roomDeletedDlq,
        roomDeletedQueue,
        roomDeletedBinding,
        roomDuplicatedFanoutExchange,
        roomDuplicatedQueue,
        roomDuplicatedBinding
    );
  }
}
