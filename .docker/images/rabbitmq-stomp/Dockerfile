FROM rabbitmq:4.1-alpine@sha256:8146e69328150e9162c7ca5bfc4b29de4df2d92179ad84d0cc29e55eb27953b5

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
