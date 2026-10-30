FROM rabbitmq:4.1-management-alpine@sha256:410a1c8844e9c9227f72735d95a391fca4559ca8b61cce9dad707ce5e2229a3a

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
