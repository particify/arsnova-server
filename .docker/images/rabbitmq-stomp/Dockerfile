FROM rabbitmq:4.3-alpine@sha256:835cbc6fabceedef7a3b0ce3195a17adccb753a3ba645bdd61c5557b6e4bf580

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
