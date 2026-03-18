FROM rabbitmq:4.2-management-alpine@sha256:4c1fde38ebd939da04ad895fc450c41516503290b6450387e5ffd43e95a34cc3

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
