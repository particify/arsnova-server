group = "net.particify.arsnova"
version = "0.0.1-SNAPSHOT"

extra["gitlabHost"] = "gitlab.com"

subprojects {
  repositories {
    mavenCentral()
  }

  tasks.register<Copy>("getDeps") {
    from(project.the<SourceSetContainer>()["main"].runtimeClasspath)
    into(layout.buildDirectory.dir("get-deps"))

    doFirst {
      delete(layout.buildDirectory.dir("get-deps"))
    }

    doLast {
      delete(layout.buildDirectory.dir("get-deps"))
    }
  }
}
