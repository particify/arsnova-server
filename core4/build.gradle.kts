plugins {
  jacoco
  alias(libs.plugins.detekt)
  alias(libs.plugins.graalvm.native)
  alias(libs.plugins.kapt)
  alias(libs.plugins.jib)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.spotless)
  alias(libs.plugins.spring.boot)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

val compose = findProperty("compose") == "true"

dependencies {
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  implementation(platform(libs.spring.modulith.bom))
  implementation(libs.kotlin.reflect)
  implementation(libs.spring.actuator)
  implementation(libs.spring.cache)
  implementation(libs.spring.graphql)
  implementation(libs.spring.jpa)
  implementation(libs.spring.modulith)
  implementation(libs.spring.security)
  implementation(libs.spring.security.jose)
  implementation(libs.spring.validation)
  implementation(libs.spring.websocket)
  implementation(libs.jackson.jsr310)
  implementation(libs.jackson.kotlin)
  implementation(libs.graphql.scalars)
  implementation(libs.querydsl.jpa)
  implementation(libs.liquibase)
  developmentOnly(libs.spring.hal.explorer)
  compileOnly(libs.postgresql)
  developmentOnly(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  developmentOnly(libs.spring.devtools)
  runtimeOnly(libs.postgresql)
  runtimeOnly(libs.spring.modulith.actuator)
  runtimeOnly(libs.spring.modulith.observability)
  testImplementation(libs.kotlin.junit)
  testImplementation(libs.spring.test) { exclude(module = "mockito-core") }
  testImplementation(libs.spring.graphql.test)
  testImplementation(libs.spring.modulith.test)
  testImplementation(libs.spring.security.test)
  testImplementation(libs.spring.testcontainers)
  testImplementation(libs.testcontainers.postgresql)
  testImplementation(libs.liquibase)
  testRuntimeOnly(libs.junit.launcher)
  kapt(variantOf(libs.querydsl.apt) { classifier("jakarta") })
  kaptTest(variantOf(libs.querydsl.apt) { classifier("jakarta") })

  if (compose) {
    developmentOnly(libs.spring.docker)
  }
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

tasks.withType<Test> { useJUnitPlatform() }

tasks.jib { jib { from { image = "eclipse-temurin:21-alpine" } } }

tasks.jacocoTestReport { reports { csv.required.set(true) } }

detekt { toolVersion = libs.versions.detekt.get() }

spotless {
  ratchetFrom("origin/master")
  kotlin {
    ktfmt(libs.versions.ktfmt.get())
    licenseHeader("/* Copyright \$YEAR Particify GmbH\n * SPDX-License-Identifier: MIT\n */")
  }
  kotlinGradle { ktfmt(libs.versions.ktfmt.get()) }
  yaml {
    target("**/*.yaml", "**/*.yml")
    targetExclude("build/**")
    prettier()
  }
  format("graphqls") {
    target("**/*.graphqls")
    targetExclude("build/**", "**/schema.graphqls")
    prettier(mapOf("prettier" to libs.versions.prettier.asProvider().get()))
  }
  format("toml") {
    target("**/*.toml")
    targetExclude("build/**")
    prettier(
            mapOf(
                "prettier" to libs.versions.prettier.asProvider().get(),
                "prettier-plugin-toml" to libs.versions.prettier.toml.get()))
        .config(mapOf("plugins" to listOf("prettier-plugin-toml")))
  }
  format("xml") {
    target("**/*.xml")
    targetExclude("build/**")
    prettier(
            mapOf(
                "prettier" to libs.versions.prettier.asProvider().get(),
                "@prettier/plugin-xml" to libs.versions.prettier.xml.get()))
        .config(mapOf("plugins" to listOf("@prettier/plugin-xml")))
  }

  tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
}
