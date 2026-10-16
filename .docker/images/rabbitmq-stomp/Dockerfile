FROM rabbitmq:4.1-alpine@sha256:922f70e0ed1bf985b658fe79d7c7e8f3c516bbb2309d7c285aa17b673d1ba953

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
