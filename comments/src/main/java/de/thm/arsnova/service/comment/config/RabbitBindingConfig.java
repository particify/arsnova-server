package de.thm.arsnova.service.comment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitBindingConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitBindingConfig.class);

    static final String commandExchangeName = "comment.command";
    static final String createCommandQueueName = "comment.command.create";
    static final String patchCommandQueueName = "comment.command.patch";
    static final String updateCommandQueueName = "comment.command.update";
    static final String deleteCommandQueueName = "comment.command.delete";
    static final String highlightCommandQueueName = "comment.command.highlight";

    static final String upvoteQueueName = "vote.command.upvote";
    static final String downvoteQueueName = "vote.command.downvote";
    static final String resetVoteQueueName = "vote.command.resetvote";

    static final Map<String, Object> commandMapper = new HashMap<String, Object>(){{
        put("create", createCommandQueueName);
        put("patch", patchCommandQueueName);
        put("update", updateCommandQueueName);
    }};

    @Bean
    @Autowired
    public Exchange exchange(RabbitAdmin rabbitAdmin) {
        final Exchange exchange = new FanoutExchange(commandExchangeName, true, false);

        try {
            rabbitAdmin.declareExchange(exchange);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return exchange;
    }

    @Bean
    @Autowired
    public Queue createCommandQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(createCommandQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    @Bean
    @Autowired
    public Queue patchCommandQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(patchCommandQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    @Bean
    @Autowired
    public Queue updateCommandQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(updateCommandQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    @Bean
    @Autowired
    public Queue deleteCommandQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(deleteCommandQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    @Bean
    @Autowired
    public Queue highlightCommandQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(highlightCommandQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    @Bean
    @Autowired
    public Queue upvoteQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(upvoteQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    @Bean
    @Autowired
    public Queue resetVoteQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(resetVoteQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    @Bean
    @Autowired
    public Queue downvoteQueueName(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue(downvoteQueueName, true, false, false);

        try {
            rabbitAdmin.declareQueue(queue);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return queue;
    }

    /*@Bean
    @Autowired
    public Binding binding(RabbitAdmin rabbitAdmin, Exchange exchange, Queue queue) {
        final Binding binding = new Binding(
                queue.getName(),
                Binding.DestinationType.QUEUE,
                exchange.getName(),
                "*",
                commandMapper
        );

        rabbitAdmin.declareBinding(binding);

        return binding;
    }*/

}
