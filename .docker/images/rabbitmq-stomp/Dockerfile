FROM rabbitmq:4.1-alpine@sha256:0cf7f9cd67a18e205978b5567d5d36c4df0eda17c3c64995f1385bec0a0a8062

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
