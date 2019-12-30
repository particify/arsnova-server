ARG IMAGE_BASETAG=latest
FROM nginx:${IMAGE_BASETAG}

RUN rm /etc/nginx/conf.d/default.conf

COPY conf.d /etc/nginx/conf.d
