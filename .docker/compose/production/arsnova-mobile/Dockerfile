ARG IMAGE_BASETAG=latest
FROM nginx:${IMAGE_BASETAG}

ARG VERSION=2.7.1

COPY conf.d /etc/nginx/conf.d

RUN \
  apk add --no-cache --virtual .build-deps \
    ca-certificates \
    curl \
    unzip \
    && \
  mkdir /opt/arsnova && \
  cd /opt/arsnova && \
  curl -sfSLO https://github.com/thm-projects/arsnova-mobile/releases/download/v$VERSION/arsnova-mobile-$VERSION.war && \
  unzip arsnova-mobile-$VERSION.war -d mobile && \
  rm arsnova-mobile-$VERSION.war && \
  rm /etc/nginx/conf.d/default.conf && \
  apk del .build-deps
