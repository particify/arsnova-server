FROM rabbitmq:4.3-management-alpine@sha256:a1a5dd841347af3e32355fd58ac530e831fd394c49e299f862e2fd4ab331cd79

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
