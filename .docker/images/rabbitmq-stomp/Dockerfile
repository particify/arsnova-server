FROM rabbitmq:4.2-alpine@sha256:0cada324382f3c592009b0c26433b295450388904497a2b4221a5ea79e279728

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
