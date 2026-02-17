FROM rabbitmq:4.2-alpine@sha256:fbf29e7cafdf7c9f7224d7f0873914d2a14ca475fafac3f800e048753096e35f

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
