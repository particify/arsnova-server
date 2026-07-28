FROM rabbitmq:4.3-alpine@sha256:c07a5e60f5429be18b3b7fd3a4dfa9a84c3372f88df084b6f2b22224192c360c

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
