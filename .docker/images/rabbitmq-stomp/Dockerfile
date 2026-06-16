FROM rabbitmq:4.3-alpine@sha256:c1bc222bedef10a300ed1e567219770a9a3d9162eca5bb36f0d528408a1aea83

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
