rootProject.name = "arsnova-server-parent"
include(
  "authz",
  "comments",
  "core",
  "gateway",
  "websocket"
)
pluginManagement {
  repositories {
    maven { url = uri("https://repo.spring.io/milestone") }
    gradlePluginPortal()
  }
}
