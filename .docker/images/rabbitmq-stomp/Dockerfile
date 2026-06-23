FROM rabbitmq:4.3-alpine@sha256:5c811610ba4502d95241cd9605fa9830028c11c0aa594e200da9b0f62632f06a

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
