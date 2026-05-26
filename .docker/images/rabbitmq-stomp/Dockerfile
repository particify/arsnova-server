FROM rabbitmq:4.3-alpine@sha256:5d6c166ea7b2dff1fc93840661d84c2c75a0d585b890c66095c430a274c7dae6

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
