FROM rabbitmq:4.2-alpine@sha256:e6d50edfddc40cbb435f26ace343a06d5ccec634d70d286e1f8535d627f9453c

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
