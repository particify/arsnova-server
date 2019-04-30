# RabbitMQ

Setup user:

```
rabbitmqctl add_user arsnova arsnova
rabbitmqctl set_user_tags arsnova administrator
rabbitmqctl set_permissions -p / arsnova ".*" ".*" ".*"
```

Enable STOMP:

`rabbitmq-plugins enable rabbitmq_stomp`

[Optional] Enable Management Plugin (localhost:15672):

`rabbitmq-plugins enable rabbitmq_management`


Naming conventions:

- https://rabbitmq.docs.pivotal.io/36/rabbit-web-docs/stomp.html

In short:

- /queue for single subscriber
- /topic for fanout subscription
- only use [a-zA-Z0-9]
