FROM rabbitmq:4.2-alpine@sha256:09fbd90e09ca0f12f0c4c40a069147412f7be25245cad774d9df5684b03a031e

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
