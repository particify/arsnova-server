FROM rabbitmq:4.3-alpine@sha256:a762c02d0c1fa7c3de1eca5d62b4fe9812649f41a3b38d716e1afcbe7e5387ad

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
