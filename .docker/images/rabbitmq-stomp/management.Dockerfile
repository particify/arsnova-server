FROM rabbitmq:4.1-management-alpine@sha256:ad4e1228e10607fede1275784ff9c791d701f8ace0364d5c9f6ac82aaaaa6fe4

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
