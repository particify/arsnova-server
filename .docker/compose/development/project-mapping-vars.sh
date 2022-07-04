#!/bin/bash
declare -A TAG_VARS PROJECT_DIRS

TAG_VARS[arsnova-web]=WEB_TAG
TAG_VARS[arsnova-http-gateway]=HTTPGW_TAG
TAG_VARS[arsnova-ws-gateway]=WSGW_TAG
TAG_VARS[arsnova-auth-service]=AUTH_TAG
TAG_VARS[arsnova-backend-core]=CORE_TAG
TAG_VARS[arsnova-comment-service]=COMMENT_TAG
TAG_VARS[arsnova-formatting-service]=FORMATTING_TAG

PROJECT_DIRS[arsnova-web]=arsnova-webclient
PROJECT_DIRS[arsnova-backend-core]=arsnova-backend
