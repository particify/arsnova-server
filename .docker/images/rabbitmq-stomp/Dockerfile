FROM rabbitmq:4.3-alpine@sha256:ae585b93b24b77f7281320c7d1e62b3098acba91eb14b1e53a9716584c95c7e9

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

COPY logging.conf /etc/rabbitmq/conf.d/90-logging.conf
COPY advanced.config /etc/rabbitmq/
