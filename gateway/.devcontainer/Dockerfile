ARG APP_IMAGE
FROM $APP_IMAGE AS build
RUN ls -l /app/*

FROM registry.gitlab.com/particify/dev/foss/devcontainers/java:0-17-bullseye

COPY --from=build /app/libs/ /app/libs/
COPY --from=build /app/ /app/

CMD /run.sh de.thm.arsnova.service.httpgateway.ArsnovaHttpGatewayServiceApplicationKt
