FROM rabbitmq:4.1-alpine@sha256:35cbf42b607746f405db5a74137fde10c21435624fb9ddaeb26f3c42be694864

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
