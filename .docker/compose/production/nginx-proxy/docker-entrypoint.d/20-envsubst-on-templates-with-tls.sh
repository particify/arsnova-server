#!/bin/sh
if [ -d /mnt/tls-certs ]; then
  echo $0: Found TLS certicates - enabling HTTPS config
  NGINX_LISTEN="443 ssl http2"
  NGINX_TLS_INCLUDE="include"
else
  echo $0: No TLS certicates found - disabling HTTPS config
  NGINX_LISTEN="80"
  NGINX_TLS_INCLUDE="#include"
fi

export NGINX_LISTEN NGINX_TLS_INCLUDE

# The entrypoint files need to be sourced so env vars can be shared
. /docker-entrypoint.d/20-envsubst-on-templates.sh
