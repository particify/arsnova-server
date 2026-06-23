FROM rabbitmq:4.3-management-alpine@sha256:63d4902023629198e692b29b37c1447327cf2c275666b918031cded01bde7e54

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
