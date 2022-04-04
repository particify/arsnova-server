#!/bin/sh
BASE_SCRIPT=/docker-entrypoint.d/20-envsubst-on-templates.sh

if [ -d /mnt/tls-certs ] && [ "$(ls -A /mnt/tls-certs)" ]; then
  echo $0: Found TLS certicates - enabling HTTPS config
  NGINX_LISTEN="443 ssl http2"
  NGINX_TLS_INCLUDE="include"
else
  echo $0: No TLS certicates found - disabling HTTPS config
  NGINX_LISTEN="80"
  NGINX_TLS_INCLUDE="#include"
fi

if [ -n "$CSP_FRAME_ANCESTORS" ]; then
  NGINX_CSP_FRAME_ANCESTORS="add_header Content-Security-Policy \"frame-ancestors $CSP_FRAME_ANCESTORS\";"
else
  NGINX_CSP_FRAME_ANCESTORS="#add_header Content-Security-Policy;"
fi

export NGINX_LISTEN NGINX_TLS_INCLUDE NGINX_CSP_FRAME_ANCESTORS

# The entrypoint files need to be sourced so env vars can be shared
echo $0: Launching $BASE_SCRIPT
. "$BASE_SCRIPT"
