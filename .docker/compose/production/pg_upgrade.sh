#!/bin/sh
set -eu

SERVICE=${1:-}
UPGRADE_VERSION=17
POSTGRES_USER=arsnova$SERVICE
UPGRADE_IMAGE=pgautoupgrade/pgautoupgrade:17-alpine
DIR=$(dirname "$0")

if [ -z "$SERVICE" ]; then
  echo "Missing argument. Usage: ./$(basename "$0") <service name>"
  exit 1
fi

COMPOSE_VERSION=$(sed -n 's/^ *image: *postgres:\([0-9]*\)[.-].*/\1/p' "$DIR/docker-compose.yml" | sort -u)
if [ "$COMPOSE_VERSION" != "$UPGRADE_VERSION" ]; then
  echo "ERROR: This script upgrades to PostgreSQL $UPGRADE_VERSION, but docker-compose.yml uses version $COMPOSE_VERSION."
  exit 1
fi

if [ -z "${COMPOSE_PROJECT_NAME:-}" ] && [ -f "$DIR/.env" ]; then
  COMPOSE_PROJECT_NAME=$(sed -n 's/^COMPOSE_PROJECT_NAME=//p' "$DIR/.env")
fi
VOLUME=${COMPOSE_PROJECT_NAME:-particify}_postgresql_data_$SERVICE

if ! docker volume inspect "$VOLUME" > /dev/null 2>&1; then
  echo "ERROR: Volume $VOLUME does not exist."
  exit 1
fi
if [ -n "$(docker ps -q --filter "volume=$VOLUME")" ]; then
  echo "ERROR: Volume $VOLUME is in use by a running container. Stop the containers first."
  exit 1
fi

OLDVER=$(docker run --rm -v "$VOLUME:/data:ro" alpine sh -c "cat /data/PG_VERSION 2> /dev/null" | tr -d '\n\r')
case $OLDVER in
  '' | *[!0-9]*)
    echo "ERROR: Failed to detect the PostgreSQL version of the data in $VOLUME."
    echo "If a previous upgrade failed, restore the volume from the backup."
    exit 1
    ;;
esac
if [ "$OLDVER" = "$UPGRADE_VERSION" ]; then
  echo "Data in $VOLUME is already on version $UPGRADE_VERSION. Nothing to do."
  exit 0
fi
if [ "$OLDVER" -gt "$UPGRADE_VERSION" ]; then
  echo "ERROR: Data in $VOLUME is on version $OLDVER, which is newer than $UPGRADE_VERSION."
  exit 1
fi

printf "Back up the volume before continuing. Upgrade %s from version %s to %s? (y/N) " "$VOLUME" "$OLDVER" "$UPGRADE_VERSION"
read -r READ_CONTINUE
if [ "$READ_CONTINUE" != y ] && [ "$READ_CONTINUE" != Y ]; then
  echo "Upgrade cancelled."
  exit 0
fi

STATUS=0
docker run --rm -v "$VOLUME:/var/lib/postgresql/data" -e PGAUTO_ONESHOT=yes -e "POSTGRES_USER=$POSTGRES_USER" "$UPGRADE_IMAGE" || STATUS=$?
NEWVER=$(docker run --rm -v "$VOLUME:/data:ro" alpine sh -c "cat /data/PG_VERSION 2> /dev/null" | tr -d '\n\r')
if [ "$STATUS" != 0 ] || [ "$NEWVER" != "$UPGRADE_VERSION" ]; then
  echo "ERROR: Upgrade failed. Restore the volume from the backup."
  exit 1
fi
echo "Upgrade completed."
