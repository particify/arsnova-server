ARG IMAGE_BASETAG=latest
FROM nginx:${IMAGE_BASETAG}

RUN rm /etc/nginx/conf.d/default.conf

COPY templates $NGINX_ENVSUBST_TEMPLATE_DIR
