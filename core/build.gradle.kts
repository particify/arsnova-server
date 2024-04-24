plugins {
  java
  jacoco
  checkstyle
  id("com.github.spotbugs")
  id("com.google.cloud.tools.jib")
  id("io.freefair.aspectj.post-compile-weaving")
  id("org.jlleitschuh.gradle.ktlint")
  id("org.springframework.boot")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://build.shibboleth.net/nexus/content/repositories/releases/")
  }
  maven {
    url = uri("https://${property("gitlabHost")}/api/v4/groups/particify/-/packages/maven")
  }
}

dependencies {
  implementation(platform(project(":platform")))
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  implementation(project(":common"))
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-mail")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework:spring-aop")
  implementation("org.springframework:spring-aspects")
  implementation("org.springframework.data:spring-data-commons")
  implementation("org.springframework.security:spring-security-aspects")
  implementation("org.springframework.security:spring-security-cas")
  implementation("org.springframework.security:spring-security-ldap")
  implementation("org.aspectj:aspectjrt")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("com.auth0:java-jwt")
  implementation("org.pac4j:pac4j-jakartaee")
  implementation("org.pac4j:pac4j-oauth")
  implementation("org.pac4j:pac4j-oidc")
  implementation("org.pac4j:pac4j-saml")
  implementation("org.ektorp:org.ektorp")
  implementation("org.ektorp:org.ektorp.spring")
  implementation("net.particify.arsnova.integrations:connector-client")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  compileOnly("org.springframework.boot:spring-boot-devtools")
  compileOnly(libs.spotbugs.annotations)
  aspect(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  aspect("org.springframework:spring-aspects")
  aspect("org.springframework.security:spring-security-aspects")
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

checkstyle {
  toolVersion = libs.versions.checkstyle.get()
  configFile = file("$projectDir/checkstyle.xml")
  configProperties =
    mapOf(
      "checkstyle.missing-javadoc.severity" to "info",
    )
  maxWarnings = 0
}

spotbugs {
  excludeFilter.set(file("../spotbugs-exclude.xml"))
}

tasks.spotbugsTest {
  enabled = false
}
