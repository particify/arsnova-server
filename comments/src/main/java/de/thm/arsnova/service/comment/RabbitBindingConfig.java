package de.thm.arsnova.service.comment;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.emptyMap;

@Configuration
public class RabbitBindingConfig {

    @Bean
    @Autowired
    public Exchange exchange(RabbitAdmin rabbitAdmin) {
        final Exchange exchange = new FanoutExchange("comment-exchange", true, false);

        rabbitAdmin.declareExchange(exchange);

        return exchange;
    }

    @Bean
    @Autowired
    public Queue queue(RabbitAdmin rabbitAdmin) {
        final Queue queue = new Queue("comment.stream", true, false, false);

        rabbitAdmin.declareQueue(queue);

        return queue;
    }

    @Bean
    @Autowired
    public Binding binding(RabbitAdmin rabbitAdmin, Exchange exchange, Queue queue) {
        final Binding binding = new Binding(queue.getName(), Binding.DestinationType.QUEUE, exchange.getName(), "#", emptyMap());

        rabbitAdmin.declareBinding(binding);

        return binding;
    }

}
