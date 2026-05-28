FROM rabbitmq:4.3-management-alpine@sha256:eb70bd64440624f8a7b93f619d28900865330996a3204aec97852bbe4bb77573

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
