FROM rabbitmq:4.2-management-alpine@sha256:3da0ceafa07a4b880d858dc2f53f6be4afe2ccef3d3582ea7d06ced6370128b8

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
