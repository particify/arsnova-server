FROM rabbitmq:4.2-management-alpine@sha256:4e91bf239d256f0343d72c34d00947292f9b08238f5645461a5e8a76adb8a89b

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
