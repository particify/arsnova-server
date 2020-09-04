ARG IMAGE_BASETAG=latest
FROM nginx:${IMAGE_BASETAG}

ENV NGINX_ENVSUBST_TEMPLATE_DIR=/etc/nginx/templates

COPY templates $NGINX_ENVSUBST_TEMPLATE_DIR
COPY dhparam tls.conf.include tls_redirect.conf /etc/nginx/
COPY docker-entrypoint.d/* /docker-entrypoint.d/

RUN \
  rm /etc/nginx/conf.d/default.conf && \
  chmod -x /docker-entrypoint.d/20-envsubst-on-templates.sh
