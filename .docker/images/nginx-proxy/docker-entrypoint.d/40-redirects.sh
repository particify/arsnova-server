#!/bin/sh
create_alias_config() {
  if [ -z "$2" ]; then
    echo $0: No alias config found for $1
    return
  fi
  export TARGET_HOSTNAME=$1
  export ALIAS_HOSTNAMES=$2
  echo $0: Creating alias redirect config for $TARGET_HOSTNAME from $ALIAS_HOSTNAMES
  envsubst '$TARGET_HOSTNAME $ALIAS_HOSTNAMES' < /etc/nginx/domain_redirect.conf.template > /etc/nginx/conf.d/${TARGET_HOSTNAME}_redirects.conf
}

create_alias_config "$ARSNOVA_HOSTNAME" "$ARSNOVA_HOSTNAME_ALIASES"

exit 0
