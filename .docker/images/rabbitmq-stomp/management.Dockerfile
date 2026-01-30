FROM rabbitmq:4.2-management-alpine@sha256:f012bdaab5c1551591bed24579ea744e01f34e2ff52c9c284ccde59bf0ec247a

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
