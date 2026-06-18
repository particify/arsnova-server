FROM rabbitmq:4.3-alpine@sha256:8489bba72d91465b2ed422394966d270858252844cc7bd91dfb8ab3dd43fdaea

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
