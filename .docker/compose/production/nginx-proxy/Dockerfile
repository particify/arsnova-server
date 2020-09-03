ARG IMAGE_BASETAG=latest
FROM nginx:${IMAGE_BASETAG}

ENV NGINX_ENVSUBST_TEMPLATE_DIR=/etc/nginx/templates

RUN rm /etc/nginx/conf.d/default.conf

COPY templates $NGINX_ENVSUBST_TEMPLATE_DIR
