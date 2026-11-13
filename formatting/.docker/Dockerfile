FROM node:26.13.0-alpine@sha256:c6ad9e9c1d80f6eb607c26b4b0a132ff7a2344fad1c78ea3baa2a7d4c8de6d94 AS build

ARG COREPACK_VERSION

WORKDIR /build

RUN npm install --global "corepack@${COREPACK_VERSION:?build argument required}" \
  && corepack enable

COPY package.json yarn.lock .yarnrc.yml ./

RUN yarn workspaces focus --production

FROM node:26.13.0-alpine@sha256:c6ad9e9c1d80f6eb607c26b4b0a132ff7a2344fad1c78ea3baa2a7d4c8de6d94

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json ./
COPY --from=build /build/node_modules node_modules/
COPY $BUILD_DIR lib

EXPOSE 3020

CMD ["node", "lib/index.js"]
