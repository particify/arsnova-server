FROM rabbitmq:4.2-management-alpine@sha256:7e3cfcd64bb49d68ae3c4b55803da8bbda368bc1c05fe5938170f13f09053897

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
