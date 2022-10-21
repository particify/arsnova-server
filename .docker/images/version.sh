#!/bin/sh
IMAGE_NAME="$1"
IMAGE_VARIANT="$2"
if [ -z "$IMAGE_NAME" ]; then
  echo No image name passed. >&2
  exit 1
fi

if [ -n "$IMAGE_VARIANT" ]; then
  DOCKERFILE="$IMAGE_VARIANT.Dockerfile"
else
  DOCKERFILE=Dockerfile
fi
IMAGES_DIR="$(dirname "$0")"
DOCKERFILE_PATH="$IMAGES_DIR/$IMAGE_NAME/$DOCKERFILE"
if [ ! -f "$DOCKERFILE_PATH" ]; then
  echo "Dockerfile ($DOCKERFILE_PATH) does not exist." >&2
  exit 1
fi
read -r DOCKER_FROM <"$DOCKERFILE_PATH"

VERSION=$(echo "$DOCKER_FROM" | sed -E 's/FROM [a-z0-9/_-]+:([^@]+).*/\1/')
printf "$VERSION"
