FROM rabbitmq:4.3-management-alpine@sha256:ee977e85c21bdc352a30e12b28eff9f4f29b00ab1773543749df3d0872a2e928

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
