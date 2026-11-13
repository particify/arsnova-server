FROM node:22.23.5-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a AS build

ARG COREPACK_VERSION

WORKDIR /build

RUN npm install --global "corepack@${COREPACK_VERSION:?build argument required}" \
  && corepack enable

COPY package.json yarn.lock .yarnrc.yml ./

RUN yarn workspaces focus --production

FROM node:22.23.5-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json ./
COPY --from=build /build/node_modules node_modules/
COPY $BUILD_DIR lib

EXPOSE 3020

CMD ["node", "lib/index.js"]
