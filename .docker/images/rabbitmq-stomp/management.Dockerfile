FROM rabbitmq:4.2-management-alpine@sha256:97aa01edc8af7a62786bc2597cb18b848a22edc13bbb47cc40d391ffaf42334b

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
