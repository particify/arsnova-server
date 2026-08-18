FROM rabbitmq:4.3-management-alpine@sha256:073c89fec6c7504703a2a709817578b2569114394c6107f6e1250681a9451a6e

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
