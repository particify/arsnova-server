FROM rabbitmq:4.1-alpine@sha256:f45d8808ab7dfbf116d343e7814590e79483fee4c5bf21070a4d43274d217791

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
