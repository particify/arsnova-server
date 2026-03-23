FROM rabbitmq:4.2-management-alpine@sha256:7983ce30209cb12a4423907cfafe9eebcfa3dfc8e7463a444b1070bf0f52851c

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
