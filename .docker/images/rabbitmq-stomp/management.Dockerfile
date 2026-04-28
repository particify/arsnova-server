FROM rabbitmq:4.2-management-alpine@sha256:cef1d5e13ce97724aa78b94e36e50fbe174a7623b8912da8b293dec1510a947f

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
