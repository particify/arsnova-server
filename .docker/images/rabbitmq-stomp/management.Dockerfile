FROM rabbitmq:4.2-management-alpine@sha256:39395a33044a845c6ae0edb44300c6c2df2e13e12ef8fb4609deb727ee85a170

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
