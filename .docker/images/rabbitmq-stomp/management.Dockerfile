FROM rabbitmq:4.3-management-alpine@sha256:0753b75ce99094c385483d89449d532a0544fb85e4942a478b21cc497ab66d33

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
