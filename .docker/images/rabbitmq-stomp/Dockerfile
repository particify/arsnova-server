FROM rabbitmq:4.1-alpine@sha256:1a087dd3a29b91448407409df70f4f6cb213ac0c269a62861bfe2a665f4ced03

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
