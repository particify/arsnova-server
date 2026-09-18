FROM rabbitmq:4.1-management-alpine@sha256:6fb3095d1d579e11639d9033c9ded1287ca085664c97e9305ffc2a26cee433f5

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
