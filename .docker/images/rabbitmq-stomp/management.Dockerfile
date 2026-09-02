FROM rabbitmq:4.3-management-alpine@sha256:6714b0f70e6b21c59fe958008a695066bf58e477595fdc77f6c8260145a62734

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
