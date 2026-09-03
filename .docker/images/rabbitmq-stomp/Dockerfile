FROM rabbitmq:4.3-alpine@sha256:3486d98205df3d6395ed70e7924baa13b561cbac54116c0ddae5b0b7382bbabd

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
