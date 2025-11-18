#!/bin/sh
IMAGE_NAME="$1"
IMAGE_VARIANT="$2"
IMAGE_AFFIX="$3"
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

VERSION=$(echo "$DOCKER_FROM" | sed -E 's/FROM ([a-z0-9./_-]+\/)?[a-z0-9_-]+:([^@]+).*/\2/')
# The base image version is more meaningful as the local version.
# => Use the local version as suffx.
IMAGE_TAG="$VERSION"
if [ -n "$IMAGE_AFFIX" ]; then
  IMAGE_TAG="${VERSION}-${IMAGE_AFFIX}"
fi
printf "$IMAGE_TAG"
