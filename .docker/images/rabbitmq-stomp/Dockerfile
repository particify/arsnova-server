FROM rabbitmq:4.3-alpine@sha256:290b4731353a388f75cfdd358f79a3f4925ab3c1e9d23394db635bcb112b3240

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
