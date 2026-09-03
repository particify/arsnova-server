FROM rabbitmq:4.3-management-alpine@sha256:ac1201d1dc227779d93c607f976e7f9156fd5b551ee5ff7946cab6c242091bf3

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
