FROM rabbitmq:4.3-management-alpine@sha256:1a43764bdcf116542e7c8c794adc67c79461727da16d474e9e21483fe7f716d3

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
