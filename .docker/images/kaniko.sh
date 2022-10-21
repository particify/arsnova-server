#!/bin/bash

set -e

if [ $# -lt 1 ]; then
    echo "Usage: run_in_docker.sh <image tag>"
    exit 1
fi

tag=$1

docker run \
    -v "$HOME/.docker:/kaniko/.docker" \
    -v "$(readlink -f $(dirname $0))/rabbitmq-stomp:/workspace" \
    gcr.io/kaniko-project/executor:latest \
    --dockerfile "/workspace/Dockerfile" \
    --destination "particify/rabbitmq-stomp:$tag" \
    --context dir:///workspace/ \
    --build-arg IMAGE_BASETAG=$tag
