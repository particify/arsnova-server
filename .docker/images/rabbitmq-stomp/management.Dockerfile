FROM rabbitmq:4.3-management-alpine@sha256:c1a9d43038bb6151877e84bb6a488493158ad5c1f21dd5d1872f347caa8053b3

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
