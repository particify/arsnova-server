FROM rabbitmq:4.3-management-alpine@sha256:e087becf34b98a6290810dbf728e938c2181596772478f249cde9a953b31145b

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
