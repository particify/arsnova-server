FROM rabbitmq:4.3-alpine@sha256:cbb4e99d0d36ee0c4785b652e9fb091b3e2cc1da0b38ef9b966f7864c544aec5

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
