FROM rabbitmq:4.3-alpine@sha256:b27345dea0332f8b05b9695cf1bcf9c03408471c0b494f4a4e9a9ad5bc990bba

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
