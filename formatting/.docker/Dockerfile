FROM node:24.16.0-alpine@sha256:fb71d01345f11b708a3553c66e7c74074f2d506400ea81973343d915cb64eef0

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json yarn.lock ./
COPY node_modules node_modules/
COPY $BUILD_DIR lib

RUN yarn --frozen-lockfile

EXPOSE 3020

CMD ["node", "lib/index.js"]
