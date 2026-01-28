FROM rabbitmq:4.2-management-alpine@sha256:86030cb65cd4947b8c3912a9a25ac6d8d71320c653165a5155089bc52491358d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
