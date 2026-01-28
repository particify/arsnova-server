FROM rabbitmq:4.2-alpine@sha256:f1e5cbe712c808536e7f5365e9f697c0c805549667713a236a8329564c5819ca

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
