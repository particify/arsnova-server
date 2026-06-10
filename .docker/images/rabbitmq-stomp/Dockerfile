FROM rabbitmq:4.3-alpine@sha256:c6303af0306d537fb35da58963dc203481bc783625e9f86ee89cd193364272e8

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
