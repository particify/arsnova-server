plugins {
  java
  jacoco
  id("com.google.cloud.tools.jib") version "3.3.0"
  id("io.spring.dependency-management") version "1.1.0"
  id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
  id("org.springframework.boot") version "2.7.4"
}

group = "net.particify.arsnova"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.auth0:java-jwt:4.2.0")
  implementation("org.postgresql:postgresql")
  implementation("org.flywaydb:flyway-core")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
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
