#!/bin/sh
KEY_EXT=_key.pem
if [ -d /mnt/tls-certs ] && [ "$(ls -A /mnt/tls-certs)" ]; then
  CERT_FILES=$(ls -1 /mnt/tls-certs/*$KEY_EXT)
  for f in $CERT_FILES; do
    export HOSTNAME=`basename $f $KEY_EXT`
    if [ -z "$NGINX_DNS_RESOLVER" ]; then
      # Use Docker's DNS resolver as default
      export NGINX_DNS_RESOLVER=127.0.0.11
    fi
    echo $0: Creating HTTPS config for $HOSTNAME
    envsubst '$HOSTNAME:$NGINX_DNS_RESOLVER' < /etc/nginx/tls.conf.include > /etc/nginx/conf.d/${HOSTNAME}_tls.conf.include
  done
  echo $0: Enabling HTTPS redirect config
  ln -sf /etc/nginx/tls_redirect.conf /etc/nginx/conf.d/
else
  echo $0: Disabling HTTPS redirect config
  rm -f /etc/nginx/conf.d/tls_redirect.conf
fi

exit 0
