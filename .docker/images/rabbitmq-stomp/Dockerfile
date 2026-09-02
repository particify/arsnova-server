FROM rabbitmq:4.3-alpine@sha256:df773a0ed4150fb473eba0689d82b7d9d839fc4e6f5c1519e123166d869b485d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
