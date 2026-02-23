FROM rabbitmq:4.2-alpine@sha256:c8a204d5b0bba833cbff44067b482b40152e8156d811a369e92fa0f1da8cb9c0

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
