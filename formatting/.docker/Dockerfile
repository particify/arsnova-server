FROM node:24.13.0-alpine@sha256:931d7d57f8c1fd0e2179dbff7cc7da4c9dd100998bc2b32afc85142d8efbc213

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json yarn.lock ./
COPY node_modules node_modules/
COPY $BUILD_DIR lib

RUN yarn --frozen-lockfile

EXPOSE 3020

CMD ["node", "lib/index.js"]
