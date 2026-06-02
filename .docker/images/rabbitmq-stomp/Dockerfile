FROM rabbitmq:4.3-alpine@sha256:7795c6c0964ef9e9675af3d78c17734ed313742795ec79fce023ea65b664cf9f

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
