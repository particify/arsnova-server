FROM rabbitmq:4.1-management-alpine@sha256:54ef5ff505352c1beef05a97ee9bd6ab95b3559a76629ba0119b1d63d8d6e4b2

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
