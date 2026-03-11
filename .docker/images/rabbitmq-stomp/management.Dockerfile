FROM rabbitmq:4.2-management-alpine@sha256:b764cbcd674d323a13eb5e1fb8e7e9025f31055cb444dd7a74d96e5981606731

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
