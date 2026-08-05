FROM rabbitmq:4.3-alpine@sha256:106888c4de019c0b54b69adc515ef7e6b625b1cfa3df4cd51bb28b519072bbd2

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
