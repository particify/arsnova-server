FROM rabbitmq:4.3-management-alpine@sha256:62ecb06c7b9fe5d6c2171d7ae9e8418ca04805b5db543af909a89e540b98a184

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
