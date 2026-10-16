FROM rabbitmq:4.1-management-alpine@sha256:72b92455b387f14d44efa3b9b4553bae3d8d82cd515376712ae08d67fc9a7b0d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
