FROM rabbitmq:4.1-management-alpine@sha256:b586ea9784425774ac56f3d4919e4f723edb3287ef8a6c4c8f0df8fe569747c3

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
