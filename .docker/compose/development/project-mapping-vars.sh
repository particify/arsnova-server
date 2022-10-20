#!/bin/bash
declare -A TAG_VARS PROJECT_DIRS

TAG_VARS[arsnova-webclient]=WEBCLIENT_TAG
TAG_VARS[arsnova-server-gateway]=GATEWAY_TAG
TAG_VARS[arsnova-server-websocket]=WEBSOCKET_TAG
TAG_VARS[arsnova-server-authz]=AUTHZ_TAG
TAG_VARS[arsnova-server-core]=CORE_TAG
TAG_VARS[arsnova-server-comments]=COMMENTS_TAG
TAG_VARS[arsnova-server-formatting]=FORMATTING_TAG

PROJECT_DIRS[arsnova-webclient]=webclient
PROJECT_DIRS[arsnova-server-gateway]=gateway
PROJECT_DIRS[arsnova-server-websocket]=websocket
PROJECT_DIRS[arsnova-server-authz]=authz
PROJECT_DIRS[arsnova-server-core]=core
PROJECT_DIRS[arsnova-server-comments]=comments
PROJECT_DIRS[arsnova-server-formatting]=formatting
