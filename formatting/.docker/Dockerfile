FROM node:26.13.0-alpine@sha256:8996bb5821d62a8fd9bb27c1cb07462f3fdce6e47d640c6a881ead0b064ce216 AS build

ARG COREPACK_VERSION

WORKDIR /build

RUN npm install --global "corepack@${COREPACK_VERSION:?build argument required}" \
  && corepack enable

COPY package.json yarn.lock .yarnrc.yml ./

RUN yarn workspaces focus --production

FROM node:26.13.0-alpine@sha256:8996bb5821d62a8fd9bb27c1cb07462f3fdce6e47d640c6a881ead0b064ce216

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json ./
COPY --from=build /build/node_modules node_modules/
COPY $BUILD_DIR lib

EXPOSE 3020

CMD ["node", "lib/index.js"]
