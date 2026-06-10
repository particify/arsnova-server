FROM rabbitmq:4.3-alpine@sha256:96f0d57adaa3f98506d8a63a7da5e4aed51c8b37107594fc0424344ed011be10

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
