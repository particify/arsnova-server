import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  jacoco
  id("com.github.spotbugs") version "5.0.14"
  id("com.google.cloud.tools.jib") version "3.3.1"
  id("io.spring.dependency-management") version "1.1.0"
  id("org.jlleitschuh.gradle.ktlint") version "11.1.0"
  id("org.springframework.boot") version "3.0.6"
  kotlin("jvm") version "1.8.20"
  kotlin("plugin.spring") version "1.8.20"
}

java.sourceCompatibility = JavaVersion.VERSION_17

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-websocket")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor:reactor-tools")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")
  implementation("com.auth0:java-jwt:4.3.0")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.ninja-squad:springmockk:4.0.0")
  compileOnly("com.github.spotbugs:spotbugs-annotations:4.7.3")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "17"
  }
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
