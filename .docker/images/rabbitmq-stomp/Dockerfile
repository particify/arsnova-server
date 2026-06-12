FROM rabbitmq:4.3-alpine@sha256:740611de227445da842c1fc8be2d00c7fb6e18df813b741a41e04f871b1e5b2d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
