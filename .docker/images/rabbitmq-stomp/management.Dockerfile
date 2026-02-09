FROM rabbitmq:4.2-management-alpine@sha256:49c7be122659b3a00ba8a31579a2545b7946dcb8392b71dab41d128c69d28493

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
