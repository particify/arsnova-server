FROM rabbitmq:4.3-alpine@sha256:4b93812804b41679198d340bfb644ff93f270a1658b28cb57dab6c85516a5da1

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
