FROM rabbitmq:4.1-management-alpine@sha256:eb6736723c5d0831ab12d29e7a2b8ee1082f744370c2a784b50b84b3b4d6d030

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
