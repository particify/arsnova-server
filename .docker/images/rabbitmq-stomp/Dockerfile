FROM rabbitmq:4.3-alpine@sha256:d363f79df91b170122c5ab6043d39d1c2612e83d333b1fc2bc84c8125272af79

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
