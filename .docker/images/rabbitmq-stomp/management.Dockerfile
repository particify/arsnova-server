FROM rabbitmq:4.1-management-alpine@sha256:218487e5c22d92a990c7ad42133b7e8784c4956f8d395cd89ce2cac6abc8a8fa

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
