FROM rabbitmq:4.4-management-alpine@sha256:fa8746831e6096fc9111365960e1e56871dd5d0d362af67fd48bd8d156aba818

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
