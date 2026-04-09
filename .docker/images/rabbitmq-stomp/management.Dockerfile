FROM rabbitmq:4.2-management-alpine@sha256:43553fb6af12bfcf0ed95fbb1c4c658d2b96eed021daf5153749a35ffb87d13d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
