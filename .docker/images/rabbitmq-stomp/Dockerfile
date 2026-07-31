FROM rabbitmq:4.1-alpine@sha256:5733d284ee87779d6f7628382cc69457d5ac82447c9e8ffba0cfeb92353e343b

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
