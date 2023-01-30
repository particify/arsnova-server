import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  jacoco
  id("com.github.spotbugs") version "5.0.13"
  id("com.google.cloud.tools.jib") version "3.3.1"
  id("io.spring.dependency-management") version "1.1.0"
  id("org.jlleitschuh.gradle.ktlint") version "11.1.0"
  id("org.springframework.boot") version "3.0.2"
  kotlin("jvm") version "1.8.0"
  kotlin("plugin.spring") version "1.8.0"
}

java.sourceCompatibility = JavaVersion.VERSION_17

extra["springCloudVersion"] = "2022.0.1"

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.cloud:spring-cloud-starter-gateway")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor:reactor-tools")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")
  implementation("com.auth0:java-jwt:4.2.2")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("io.projectreactor:reactor-test")
  compileOnly("com.github.spotbugs:spotbugs-annotations:4.7.3")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
  }
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
