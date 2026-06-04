FROM rabbitmq:4.3-management-alpine@sha256:0c1330bfbaa544292d132eabe8f07bce4ec6f4817c0ca95bec0f2ba87483913f

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
