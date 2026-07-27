FROM rabbitmq:4.3-management-alpine@sha256:c511562a12d3299f760b213d8e4454919840afc73dab21f398479988d460b4ce

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
