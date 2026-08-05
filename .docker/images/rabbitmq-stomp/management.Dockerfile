FROM rabbitmq:4.3-management-alpine@sha256:cd624335f752f704e768239ea21501e5771ca13b3b278520da5ad1076eb86e55

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
