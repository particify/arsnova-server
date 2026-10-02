FROM rabbitmq:4.1-alpine@sha256:6a0f7984185cd2cdbf78a184b4c17abe88ecbbda6006a7acf17dbc7e4307f4e2

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
