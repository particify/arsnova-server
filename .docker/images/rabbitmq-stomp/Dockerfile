FROM rabbitmq:4.2-alpine@sha256:ae39273718413123950b4611438c49b9cc5290951c9224f4c9bce4c6347fe8ab

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
