FROM rabbitmq:4.2-alpine@sha256:53e51f8469ef13d0a1a2b03c0d36dcf4efbf13089f552440ec5620ca07f2c64e

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
