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

VERSION=$(echo "$DOCKER_FROM" | sed -E 's/FROM [a-z0-9/_-]+:([^@]+).*/\1/')
if [ "$IMAGE_NAME" = arsnova-proxy ]; then
  # The local version is more meaningful as the base image version.
  # => Always include the local version in the tag name.
  # => Use the local version it as prefix.
  BASE_IMAGE_NAME=$(echo "$DOCKER_FROM" | sed -E 's/FROM ([a-z0-9/_-]+):[^@]+.*/\1/')
  if [ -z "$IMAGE_AFFIX" ]; then
    IMAGE_AFFIX=next
  fi
  IMAGE_TAG="${IMAGE_AFFIX}_${BASE_IMAGE_NAME}-${VERSION}"
else
  # The base image version is more meaningful as the local version.
  # => Use the local version as suffx.
  IMAGE_TAG="$VERSION"
  if [ -n "$IMAGE_AFFIX" ]; then
    IMAGE_TAG="${VERSION}_${IMAGE_AFFIX}"
  fi
fi
printf "$IMAGE_TAG"
