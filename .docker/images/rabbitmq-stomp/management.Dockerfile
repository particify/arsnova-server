FROM rabbitmq:4.3-management-alpine@sha256:b84c252f54ecd61522320135c2bfe19a8db3e49e8dd559737817e8429e84cb7e

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
