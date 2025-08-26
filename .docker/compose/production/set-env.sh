#!/bin/sh
ENV=$1
ENV_FILE=docker-compose.$ENV.yml
if [ ! -f "$ENV_FILE" ]; then
  echo No configuration for environment \"$ENV\" found.
  exit 1
fi
ln -sf docker-compose.$ENV.yml docker-compose.override.yml
ln -sf .$ENV.env .env
