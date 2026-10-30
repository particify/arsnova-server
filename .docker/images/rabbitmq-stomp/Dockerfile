FROM rabbitmq:4.1-alpine@sha256:153322e3974e6365c50e523e5eadd5a7fae231f0e8c58f67c03bb3e02056e34e

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
