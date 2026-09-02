FROM rabbitmq:4.3-management-alpine@sha256:5b6a50b2f1dbd987bb1a6a9e20b152910c3dc8ae32e1c9060b543ecd9250f6b9

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
