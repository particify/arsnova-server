FROM rabbitmq:4.2-alpine@sha256:f5efafa65aada6282656e2b5e61fca04de994c55559dbc1622dee0f05e3cb928

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
