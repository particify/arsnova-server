FROM rabbitmq:4.3-management-alpine@sha256:a2b8ca223e4b6b91ce6dac5a87e8d4551974a7d8dc8c919d333b757507966ffd

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
