FROM rabbitmq:4.2-management-alpine@sha256:b618738ea52cb6d0073ee9f0412ede133411ecacd7afa40f17be748a6d9a9ee1

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
