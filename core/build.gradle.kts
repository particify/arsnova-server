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

java.sourceCompatibility = JavaVersion.VERSION_17

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
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
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
  implementation("org.aspectj:aspectjrt:${property("aspectjRtVersion")}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("com.auth0:java-jwt:${property("javaJwtVersion")}")
  implementation("org.pac4j:pac4j-jakartaee:${property("pac4jVersion")}")
  implementation("org.pac4j:pac4j-oauth:${property("pac4jVersion")}")
  implementation("org.pac4j:pac4j-oidc:${property("pac4jVersion")}")
  implementation("org.pac4j:pac4j-saml:${property("pac4jVersion")}")
  implementation("org.ektorp:org.ektorp:${property("ektorpVersion")}")
  implementation("org.ektorp:org.ektorp.spring:${property("ektorpVersion")}")
  implementation("org.graalvm.js:js:${property("graalvmVersion")}")
  implementation("org.graalvm.js:js-scriptengine:${property("graalvmVersion")}")
  implementation("net.particify.arsnova.integrations:connector-client:${property("connectorClientVersion")}")
  implementation("io.micrometer:micrometer-registry-prometheus")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  compileOnly("org.springframework.boot:spring-boot-devtools")
  compileOnly("com.github.spotbugs:spotbugs-annotations:${property("spotbugsAnnotationsVersion")}")
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
      image = "eclipse-temurin:17-alpine"
    }
  }
}

tasks.jacocoTestReport {
  reports {
    csv.required.set(true)
  }
}

checkstyle {
  toolVersion = "${property("checkstyleVersion")}"
  configFile = file("$projectDir/checkstyle.xml")
  configProperties = mapOf(
    "checkstyle.missing-javadoc.severity" to "info"
  )
  maxWarnings = 0
}

spotbugs {
  excludeFilter.set(file("../spotbugs-exclude.xml"))
}

tasks.spotbugsTest {
  enabled = false
}
