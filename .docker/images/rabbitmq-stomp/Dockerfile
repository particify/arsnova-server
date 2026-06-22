FROM rabbitmq:4.3-alpine@sha256:36941a6d0e07669181d0bfed70eb78d2a650d81cd5da770a06d49eec461e88f8

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
