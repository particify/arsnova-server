pluginManagement { includeBuild("build-logic") }

dependencyResolutionManagement {
  versionCatalogs {
    create("passwordDictionaries") { from(files("gradle/password-dictionaries.versions.toml")) }
  }
}

rootProject.name = "arsnova-server-parent"
include(
  "platform",
  "common",
  "authz",
  "comments",
  "core",
  "core4",
  "websocket"
)
