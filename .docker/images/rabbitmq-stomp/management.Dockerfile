FROM rabbitmq:4.3-management-alpine@sha256:888779c9c29e65c7a57e4938f38ff807057cedf76f8bfa5298f5aab7569a5a62

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
