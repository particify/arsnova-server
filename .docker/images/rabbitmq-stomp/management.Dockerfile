FROM rabbitmq:4.2-management-alpine@sha256:7c5fe2a75e711d6012c9c407748cf0b4db8474207a507e4549bbf2342bf711ac

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
