FROM rabbitmq:4.2-management-alpine@sha256:bbf30d6c3602243fd847a8883745026cdea9eb071fd5b9f2213e1317a40e5a3e

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
