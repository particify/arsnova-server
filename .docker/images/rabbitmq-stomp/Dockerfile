FROM rabbitmq:4.1-alpine@sha256:3dd4ce7606f9fbf2e2dbca90a02f29c19c5cdf66bf15cdf5237430e3614128ca

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
