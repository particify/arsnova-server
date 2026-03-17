FROM rabbitmq:4.2-alpine@sha256:449cbacbdb598bd748b98b95e0db590e3981189c7d1fc2dbe10059ef98642b3d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
