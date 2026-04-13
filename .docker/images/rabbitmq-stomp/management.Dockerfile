FROM rabbitmq:4.2-management-alpine@sha256:639e86878e0c0475a03c15444795c7ee25df0aec4ba7ad46a5a1e073d33abb22

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
