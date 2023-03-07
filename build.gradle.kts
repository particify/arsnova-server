group = "net.particify.arsnova"
version = "0.0.1-SNAPSHOT"

extra["gitlabHost"] = "gitlab.com"

plugins {
  id("com.github.spotbugs") version "5.0.13" apply false
  id("com.google.cloud.tools.jib") version "3.3.1" apply false
  id("io.freefair.aspectj.post-compile-weaving") version "6.6.3" apply false
  id("org.jlleitschuh.gradle.ktlint") version "11.3.1" apply false
  id("org.springframework.boot") version "3.0.4" apply false
  kotlin("jvm") version "1.8.10" apply false
  kotlin("plugin.spring") version "1.8.10" apply false
}

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
