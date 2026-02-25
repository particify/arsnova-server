FROM rabbitmq:4.2-management-alpine@sha256:9651ce95332b3774065981d2a65e26edf259dd7a1bea387712e73095a1cec185

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
