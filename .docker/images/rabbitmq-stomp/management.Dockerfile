FROM rabbitmq:4.1-management-alpine@sha256:924dc019a663a425e87197c6129942cacab2ec6c2b81bf9b1441993bda4b297d

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
