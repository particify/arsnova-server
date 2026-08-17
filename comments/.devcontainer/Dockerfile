ARG APP_IMAGE
FROM $APP_IMAGE AS build
RUN ls -l /app/*

FROM registry.gitlab.com/particify/dev/foss/devcontainers/java-postgresql:3-25-trixie

RUN apt-get update \
  && apt-get install -y --no-install-recommends pre-commit \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/libs/ /app/libs/
COPY --from=build /app/ /app/

CMD /run.sh net.particify.arsnova.comments.CommentsApplication
