FROM rabbitmq:4.2-management-alpine@sha256:adac51a4a14a200b8eb928a12787564ed56e93fc55e789a73ec13e1e2eac7aef

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
