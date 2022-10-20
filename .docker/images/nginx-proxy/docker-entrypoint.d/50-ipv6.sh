#!/bin/sh

# check if we have ipv6 available
if [ ! -f "/proc/net/if_inet6" ]; then
  echo >&3 "$0: info: Disable ipv6 configs"
  CONF_FILES=$(ls -1 /etc/nginx/*.conf /etc/nginx/conf.d/*.conf)
  for f in $CONF_FILES; do
    sed -i -E 's/^(\s*)(listen \[::\])/\1#\2/g' "$f"
  done
fi

exit 0
