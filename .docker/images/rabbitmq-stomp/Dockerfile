ARG IMAGE_BASETAG=alpine
FROM rabbitmq:${IMAGE_BASETAG}

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
