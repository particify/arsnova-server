FROM rabbitmq:4.2-alpine@sha256:a1838d22d0917aca59dcf9b7baed4783c2d8cb17f1b8109f32fb45396cd49ef3

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
