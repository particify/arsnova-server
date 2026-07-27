FROM rabbitmq:4.1-management-alpine@sha256:83c020e4d96cafa0a11693f5b9bcb20cd51d222edebd54aa584dd767e009f5d5

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
