FROM rabbitmq:4.2-management-alpine@sha256:4ba40e715b16ade72116073ba8d4ab3cd03dc6eb4e785a92d82f329f7292135f

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
