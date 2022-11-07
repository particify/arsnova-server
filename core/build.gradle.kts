plugins {
  java
  jacoco
  checkstyle
  id("com.google.cloud.tools.jib") version "3.3.1"
  id("io.freefair.aspectj.post-compile-weaving") version "6.5.1"
  id("io.spring.dependency-management") version "1.1.0"
  id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
  id("org.springframework.boot") version "3.0.0-RC1"
}

group = "net.particify.arsnova"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

extra["gitlabHost"] = "gitlab.com"

repositories {
  mavenCentral()
  maven {
    url = uri("https://repo.spring.io/milestone")
  }
  maven {
    url = uri("https://build.shibboleth.net/nexus/content/repositories/releases/")
  }
  maven {
    url = uri("https://${property("gitlabHost")}/api/v4/groups/particify/-/packages/maven")
  }
}

dependencies {
  implementation("com.sun.mail:jakarta.mail")
  implementation("commons-lang:commons-lang:2.6")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework:spring-aop")
  implementation("org.springframework:spring-aspects")
  implementation("org.springframework.data:spring-data-commons")
  implementation("org.springframework.security:spring-security-aspects")
  implementation("org.springframework.security:spring-security-ldap")
  implementation("org.aspectj:aspectjrt:1.9.9.1")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("com.auth0:java-jwt:4.2.1")
  implementation("org.pac4j:pac4j-jakartaee:5.6.1")
  implementation("org.pac4j:pac4j-oauth:5.6.1")
  implementation("org.pac4j:pac4j-oidc:5.6.1")
  implementation("org.pac4j:pac4j-saml:5.6.1")
  implementation("org.ektorp:org.ektorp:1.5.0")
  implementation("org.ektorp:org.ektorp.spring:1.5.0")
  implementation("org.graalvm.js:js:22.3.0")
  implementation("org.graalvm.js:js-scriptengine:22.3.0")
  implementation("net.particify.arsnova.integrations:connector-client:1.0.2")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  compileOnly("org.springframework.boot:spring-boot-devtools")
  aspect("org.springframework:spring-aspects")
  aspect("org.springframework.security:spring-security-aspects")
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

checkstyle {
  toolVersion = "10.3.4"
  configFile = file("$projectDir/checkstyle.xml")
  configProperties = mapOf(
    "checkstyle.missing-javadoc.severity" to "info"
  )
  maxWarnings = 0
}
