FROM rabbitmq:4.3-management-alpine@sha256:00ac23fd9e98cc5bb30e1217e2565d80ce85ab30ca87a5dc00ddb63fc907d3bb

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
