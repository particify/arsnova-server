group = "net.particify.arsnova"
version = "0.0.1-SNAPSHOT"

extra["gitlabHost"] = "gitlab.com"

plugins {
  alias(libs.plugins.aspectj) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.jib) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.jpa) apply false
  alias(libs.plugins.kotlin.spring) apply false
  alias(libs.plugins.ktlint) apply false
  alias(libs.plugins.spotbugs) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.spring.boot) apply false
}

subprojects {
  repositories {
    mavenCentral()
  }
}

tasks.register<Task>(name = "getDeps") {
  description = "Resolve and prefetch dependencies"
  doLast {
    rootProject.allprojects.forEach {
      it.buildscript.configurations.filter(Configuration::isCanBeResolved).forEach { it.resolve() }
      it.configurations.filter(Configuration::isCanBeResolved).forEach { it.resolve() }
    }
  }
}
