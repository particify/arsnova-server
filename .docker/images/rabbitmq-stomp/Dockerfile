FROM rabbitmq:4.3-alpine@sha256:0bbdf3288611f8b2dc12fd0215600fc20323b2fcc23bbc661c80d89c00c9580b

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
