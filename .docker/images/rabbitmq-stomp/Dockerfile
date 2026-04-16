FROM rabbitmq:4.2-alpine@sha256:a7dc778fd123325276d1f9b49a1e14a1262ce3ef69543f1f394b845c8d84ebe8

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
