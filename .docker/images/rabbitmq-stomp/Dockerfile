FROM rabbitmq:4.3-alpine@sha256:9ff97cea7d9b914b69eccad888fae70d7a86c799d99081fdf7e5da9e17e1769c

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
