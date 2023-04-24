plugins {
  java
  jacoco
  id("com.github.spotbugs") version "5.0.14"
  id("com.google.cloud.tools.jib") version "3.3.1"
  id("io.spring.dependency-management") version "1.1.0"
  id("org.jlleitschuh.gradle.ktlint") version "11.1.0"
  id("org.springframework.boot") version "3.0.6"
}

java.sourceCompatibility = JavaVersion.VERSION_17

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.auth0:java-jwt:4.3.0")
  implementation("org.postgresql:postgresql")
  implementation("org.flywaydb:flyway-core")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  compileOnly("com.github.spotbugs:spotbugs-annotations:4.7.3")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.jib {
  jib {
    from {
      image = "eclipse-temurin:17-alpine"
    }
  }
}

tasks.jacocoTestReport {
  reports {
    csv.isEnabled = true
  }
}

spotbugs {
  excludeFilter.set(file("../spotbugs-exclude.xml"))
}

tasks.spotbugsTest {
  enabled = false
}
