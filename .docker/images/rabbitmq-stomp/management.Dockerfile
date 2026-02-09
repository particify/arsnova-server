FROM rabbitmq:4.2-management-alpine@sha256:d35629711d2dcc90fd0428210da72696bf24684e3f58933e7d9abcf55148b9cd

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
