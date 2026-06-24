FROM rabbitmq:4.3-management-alpine@sha256:e6862a7d804c80fc72d2265aed3b725fd9a6afd330b735363eeecc4b6ec4dd3d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
