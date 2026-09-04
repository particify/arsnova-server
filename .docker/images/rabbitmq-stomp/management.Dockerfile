FROM rabbitmq:4.1-management-alpine@sha256:9cee09b738ad0a9ff1b5bca1883313b745ddfe9b24528d398ca489c34b0feb55

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
