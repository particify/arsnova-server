FROM rabbitmq:4.2-management-alpine@sha256:f33dc8cef83be952791356850d88b965111d11ec736133b58acc5e5103efc9f4

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
