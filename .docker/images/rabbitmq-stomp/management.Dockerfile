FROM rabbitmq:4.3-management-alpine@sha256:8724c6573b43d315bcec914f16a509d0b5faf5f3b281a832b484ece0bbcbe914

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
