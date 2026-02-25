FROM rabbitmq:4.2-management-alpine@sha256:ffb3b678cb5325932abdaf195eedff89003d901e44eae811f1272add969af295

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
