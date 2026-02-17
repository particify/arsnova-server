FROM rabbitmq:4.2-management-alpine@sha256:17b4655865f05ae546bf724d3a42841bc35afe7377be6442873e9a5472521287

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
