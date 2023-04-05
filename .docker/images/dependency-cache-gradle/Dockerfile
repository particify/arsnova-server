ARG BASE_TAG
FROM eclipse-temurin:$BASE_TAG

ARG GRADLE_DEPENDENCY_DIR

RUN apk add --no-cache git \
  && rm -rf /var/cache/apk/*

COPY $GRADLE_DEPENDENCY_DIR /root/.gradle-ro-cache/modules-2
