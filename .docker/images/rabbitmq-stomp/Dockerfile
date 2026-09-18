FROM rabbitmq:4.1-alpine@sha256:8beb027ddbd43c724978ad898933eeef3baee18d1637440df59edf22d4900091

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
