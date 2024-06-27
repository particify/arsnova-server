plugins {
  java
  jacoco
  id("com.github.spotbugs")
  id("com.google.cloud.tools.jib")
  id("org.jlleitschuh.gradle.ktlint")
  id("org.springframework.boot")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

dependencies {
  implementation(platform(project(":platform")))
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  implementation(project(":common"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.auth0:java-jwt")
  implementation("org.postgresql:postgresql")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  compileOnly(libs.spotbugs.annotations)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.jib {
  jib {
    from {
      image = "eclipse-temurin:21-alpine"
    }
  }
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
