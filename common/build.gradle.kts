plugins {
  java
  jacoco
  id("com.github.spotbugs")
  id("org.jlleitschuh.gradle.ktlint")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

dependencies {
  implementation(platform(project(":platform")))
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  implementation("org.springframework:spring-context")
  implementation("org.springframework:spring-core")
  implementation("com.google.guava:guava")
  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("org.slf4j:slf4j-api")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.jacocoTestReport {
  reports {
    csv.required.set(true)
  }
}

spotbugs {
  excludeFilter.set(file("../spotbugs-exclude.xml"))
}

tasks.spotbugsTest {
  enabled = false
}
