FROM rabbitmq:4.2-alpine@sha256:eb0bf75f2749de101492a7d3b031095e903fbb02aca3baa34dbaa27589c22efb

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
