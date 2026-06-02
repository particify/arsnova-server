FROM rabbitmq:4.3-alpine@sha256:864c7f33ec3ff066de8c66ab337f39258d00f8680f1ef3f14138be788463ca9f

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
