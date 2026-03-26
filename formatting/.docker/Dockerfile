FROM node:24.14.1-alpine@sha256:01743339035a5c3c11a373cd7c83aeab6ed1457b55da6a69e014a95ac4e4700b

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json yarn.lock ./
COPY node_modules node_modules/
COPY $BUILD_DIR lib

RUN yarn --frozen-lockfile

EXPOSE 3020

CMD ["node", "lib/index.js"]
