FROM rabbitmq:4.1-alpine@sha256:f8d80e5617a335d4a5c9b01cd3387e8aef5a0df72b75c4759da36d3b258ef962

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
