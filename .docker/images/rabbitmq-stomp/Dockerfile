FROM rabbitmq:4.4-alpine@sha256:3dbd31cb9e4d45dac44de7b73cc45b848028494e98bcab71477386cbd4dc7f56

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
