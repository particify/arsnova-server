FROM rabbitmq:4.3-alpine@sha256:fe9b97699f9f6adeba3bb8d6817f86b76ff8c5ec94764213706df7e69ac94821

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
