FROM rabbitmq:4.3-alpine@sha256:637bfab9701dc52125fea6fad483d0cd97da8108aa67bbfd242318bcd2c44e38

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
