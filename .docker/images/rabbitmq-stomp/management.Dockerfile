FROM rabbitmq:4.2-management-alpine@sha256:426aa8163d714142a902fe50cca1af49d75fb2351863331647c1976339f49809

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
