FROM nginx:1.23-alpine

ENV NGINX_ENVSUBST_TEMPLATE_DIR=/etc/nginx/templates

COPY nginx.conf /etc/nginx/
COPY templates $NGINX_ENVSUBST_TEMPLATE_DIR
COPY dhparam domain_redirect.conf.template tls.conf.include tls_redirect.conf /etc/nginx/
COPY legacy_redirect.conf.include /etc/nginx/conf.d/
COPY docker-entrypoint.d/* /docker-entrypoint.d/

RUN \
  rm /etc/nginx/conf.d/default.conf && \
  chmod -x /docker-entrypoint.d/20-envsubst-on-templates.sh
