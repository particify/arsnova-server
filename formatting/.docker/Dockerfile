FROM node:24.22.2-alpine@sha256:a34ab6d7f6f4813f4d0c5469d74685604b962fbdbf5793a7bc7b4998ecb867c8 AS build

ARG COREPACK_VERSION

WORKDIR /build

RUN npm install --global "corepack@${COREPACK_VERSION:?build argument required}" \
  && corepack enable

COPY package.json yarn.lock .yarnrc.yml ./

RUN yarn workspaces focus --production

FROM node:24.22.2-alpine@sha256:a34ab6d7f6f4813f4d0c5469d74685604b962fbdbf5793a7bc7b4998ecb867c8

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json ./
COPY --from=build /build/node_modules node_modules/
COPY $BUILD_DIR lib

EXPOSE 3020

CMD ["node", "lib/index.js"]
