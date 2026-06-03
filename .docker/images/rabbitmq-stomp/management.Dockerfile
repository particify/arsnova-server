FROM rabbitmq:4.3-management-alpine@sha256:238fbbfda11c82528c8daed8c7e6aa0d5ba271d862028c544b3cacfed32891f7

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
