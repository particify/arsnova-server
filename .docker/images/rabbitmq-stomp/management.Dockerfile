FROM rabbitmq:4.2-management-alpine@sha256:97c0b8cf0b392c6742cce3795e14cb9b3443c833b3d967449d1ec1e8db1cb2d3

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
