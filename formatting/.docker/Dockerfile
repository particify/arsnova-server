FROM node:24.22.1-alpine@sha256:feb76650b7195319268c59cb2d419c732d6c499b13bf5296f16ab59eb1f0a4f3

ENV BUILD_DIR=lib
ENV NODE_ENV=production

WORKDIR /opt/particify/formatting-service

COPY package.json yarn.lock ./
COPY node_modules node_modules/
COPY $BUILD_DIR lib

RUN yarn --frozen-lockfile

EXPOSE 3020

CMD ["node", "lib/index.js"]
