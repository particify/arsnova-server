FROM rabbitmq:4.3-management-alpine@sha256:bd84d43f2162c3377e0be05f73cafd988359badffb86f17fbac735f607b3c6c2

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
