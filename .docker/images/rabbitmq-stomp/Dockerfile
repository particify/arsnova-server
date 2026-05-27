FROM rabbitmq:4.3-alpine@sha256:330154ff7e5cb2e0642df79c0000884b59031c786eea3111fc6c4b48a160f204

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
