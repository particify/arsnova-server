FROM rabbitmq:4.3-management-alpine@sha256:e2f08f846de10bb09649a8b020f286ed362a8f72ee45e5a8d043851f1533fda8

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
