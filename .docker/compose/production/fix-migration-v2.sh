#!/bin/sh
if docker compose >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker compose"
else
  DOCKER_COMPOSE_CMD="docker-compose"
fi

$DOCKER_COMPOSE_CMD exec postgresql_comment psql -U arsnovacomment arsnovacomment -c "UPDATE flyway_schema_history SET checksum = '-357757562' WHERE version = '2' AND checksum = '1378544780';"
