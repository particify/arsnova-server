FROM rabbitmq:4.3-alpine@sha256:89444b4c97a5469ea5300ecb7e55d32a0c1552102e562131b40aea84aafc97d1

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
