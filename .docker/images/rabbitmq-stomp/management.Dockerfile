FROM rabbitmq:4.3-management-alpine@sha256:23a53f833b333b301d48a5be9b9946e104c3c30f1620e5423adc84cdefed7366

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
