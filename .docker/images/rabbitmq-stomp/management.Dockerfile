FROM rabbitmq:4.2-management-alpine@sha256:d1245c056d85a0fe8de57954150de95ad20f57cbe6696622a92787c381c6f46b

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
