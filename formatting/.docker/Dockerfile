FROM node:24.21.0-alpine@sha256:4b2d7eef36889f0aec0d58d1b19778321176c67824b7c951352d86c5c7811d44

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json yarn.lock ./
COPY node_modules node_modules/
COPY $BUILD_DIR lib

RUN yarn --frozen-lockfile

EXPOSE 3020

CMD ["node", "lib/index.js"]
