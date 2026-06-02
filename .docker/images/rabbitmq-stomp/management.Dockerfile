FROM rabbitmq:4.3-management-alpine@sha256:7e12d10b866cee4a93e4ca058bf98592f08f9fffd6dda486f29a92861b149348

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
