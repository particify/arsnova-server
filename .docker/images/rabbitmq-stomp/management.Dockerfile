FROM rabbitmq:4.1-management-alpine@sha256:fd7e47e224648a33d730674ac658f4541dfefadbb38de916c379679091c97b84

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
