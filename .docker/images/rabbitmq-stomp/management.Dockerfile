FROM rabbitmq:4.3-management-alpine@sha256:2f8a91462dc3c9eb11d2ddf075d3eba2201ccb75beceb3b6166dec450f412d01

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
