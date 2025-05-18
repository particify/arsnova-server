plugins {
  jacoco
  alias(libs.plugins.detekt)
  alias(libs.plugins.jib)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.spotless)
  alias(libs.plugins.spring.boot)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

dependencies {
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  implementation(platform(libs.spring.modulith.bom))
  implementation(libs.kotlin.reflect)
  implementation(libs.spring.modulith)
  implementation(libs.spring.web)
  testImplementation(libs.kotlin.junit)
  testImplementation(libs.spring.test) { exclude(module = "mockito-core") }
  testImplementation(libs.spring.modulith.test)
  testRuntimeOnly(libs.junit.launcher)
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
}
