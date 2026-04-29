FROM rabbitmq:4.3-alpine@sha256:4f4a74fe951bb8bebae41187c8592ac0737e63d929467502f17689b22e9e9204

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
