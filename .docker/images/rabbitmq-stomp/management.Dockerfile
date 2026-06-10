FROM rabbitmq:4.3-management-alpine@sha256:6bceea29dc14e7b71e3361b38d82334c5bb364c6762eb08c6add3b8dd8ea9839

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
