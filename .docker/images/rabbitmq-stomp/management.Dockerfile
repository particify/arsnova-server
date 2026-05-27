FROM rabbitmq:4.3-management-alpine@sha256:7f0e22dce6a456d963b6a96ef785fafa308c4e35838c9d64c95ba24a83058cce

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
