FROM rabbitmq:4.1-management-alpine@sha256:8956b9bc9f77b06485c925f36b4a2ea5c83816de1d4cac79c9bb484e6e71e1d0

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
