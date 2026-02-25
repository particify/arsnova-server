FROM rabbitmq:4.2-management-alpine@sha256:b12db72695ce42b658861ee0a9f2091dd46cf8c2fc82fb2ddbd69a1e502645eb

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
