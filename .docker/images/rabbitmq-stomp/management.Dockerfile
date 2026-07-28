FROM rabbitmq:4.3-management-alpine@sha256:c92a0990c19935e92a8c3e6d0b6062e44191e88e14e34bca7c1af241af6b5fb7

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
