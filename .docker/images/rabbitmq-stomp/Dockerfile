FROM rabbitmq:4.2-alpine@sha256:e1edf85e1382f3363c237542add11ecdc7e6e731a4ac19bc76206cf61d43a12d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
