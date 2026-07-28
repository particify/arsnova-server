FROM rabbitmq:4.3-alpine@sha256:0e2a14c4af3ea712f0dd440b3a6ec4a75200dd7e9a7454859b058b8a504165b6

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
