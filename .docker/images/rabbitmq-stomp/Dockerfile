FROM rabbitmq:4.3-alpine@sha256:cbcb4b54ab4fed7a87cf6beba834222eae15fe85848ce6a970e097f3434a6432

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
