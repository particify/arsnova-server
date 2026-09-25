FROM rabbitmq:4.1-management-alpine@sha256:4631fc431a8fb41e3d7449f9a18faa13ea807793b7a5224080ff505780f8d11b

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
