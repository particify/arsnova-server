ARG APP_IMAGE
FROM $APP_IMAGE AS build
RUN ls -l /app/*

FROM registry.gitlab.com/particify/dev/foss/devcontainers/java-postgresql:1-21-bookworm

COPY --from=build /app/libs/ /app/libs/
COPY --from=build /app/ /app/

CMD /run.sh net.particify.arsnova.authz.AuthzApplicationKt
