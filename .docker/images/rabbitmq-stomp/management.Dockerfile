FROM rabbitmq:4.2-management-alpine@sha256:9a2ad4f5472b616e2985ed91cf51ac9bb7b76806303f5e92a279702f93d02c1f

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
