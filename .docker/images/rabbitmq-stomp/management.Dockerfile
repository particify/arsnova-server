FROM rabbitmq:4.3-management-alpine@sha256:002a875e96365eeed67be445becfd240947ca82279103b592a859ea7a8e2a746

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
