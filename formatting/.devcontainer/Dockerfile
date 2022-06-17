ARG APP_IMAGE
FROM $APP_IMAGE AS build
RUN ls -l /opt/particify/formatting-service/*

FROM registry.gitlab.com/particify/dev/foss/devcontainers/javascript-node:0-16-bullseye

COPY --from=build /opt/particify/formatting-service/node_modules/ /app/node_modules/
COPY --from=build /opt/particify/formatting-service/ /app/

CMD /run.sh lib/index.js
