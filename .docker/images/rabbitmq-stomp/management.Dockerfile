FROM rabbitmq:4.1-management-alpine@sha256:88cc4b1af4ced2b5be575a4de258da9b6bd1685fc4828f8e3195e38dfcbf1760

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
