#!/bin/sh
KEY_EXT=_key.pem
if [ -d /mnt/tls-certs ]; then
  CERT_FILES=$(ls -1 /mnt/tls-certs/*$KEY_EXT)
  for f in $CERT_FILES; do
    export HOSTNAME=`basename $f $KEY_EXT`
    echo $0: Creating HTTPS config for $HOSTNAME
    envsubst '$HOSTNAME' < /etc/nginx/tls.conf.include > /etc/nginx/conf.d/${HOSTNAME}_tls.conf.include
  done
  echo $0: Enabling HTTPS redirect config
  ln -sf /etc/nginx/tls_redirect.conf /etc/nginx/conf.d/
else
  echo $0: Disabling HTTPS redirect config
  rm /etc/nginx/conf.d/tls_redirect.conf
fi
