FROM rabbitmq:4.2-management-alpine@sha256:1d3ca02a9fc930e576973a6eb11a22ecb5618e77fbdab31987829422e5eba654

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
