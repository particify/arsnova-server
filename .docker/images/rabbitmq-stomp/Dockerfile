FROM rabbitmq:4.1-alpine@sha256:33335c41151058d46086c04859e6b91bfdf97f9575efd3ca9709a20b5cd45aee

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
