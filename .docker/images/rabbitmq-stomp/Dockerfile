FROM rabbitmq:4.2-alpine@sha256:dce0e7c180848a90de9f9fcbd2696780e88af0fe6ddf21d950f9e003a64ef50c

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
