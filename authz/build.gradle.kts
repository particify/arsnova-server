plugins {
  jacoco
  id("com.github.spotbugs")
  id("com.google.cloud.tools.jib")
  id("org.jlleitschuh.gradle.ktlint")
  id("org.springframework.boot")
  kotlin("jvm")
  kotlin("plugin.jpa")
  kotlin("plugin.spring")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

val debugHost = System.getenv("DEBUG_HOST") ?: "127.0.0.1"
val debugPort = System.getenv("DEBUG_PORT")?.toInt() ?: 5005

dependencies {
  implementation(platform(project(":platform")))
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
  implementation(project(":common"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-tomcat")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.security:spring-security-oauth2-jose")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor:reactor-tools")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("org.postgresql:postgresql")
  implementation("org.flywaydb:flyway-database-postgresql")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.amqp:spring-rabbit-test")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.h2database:h2")
  compileOnly(libs.spotbugs.annotations)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.bootRun {
  debugOptions {
    host = debugHost
    port = debugPort
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.jib {
  jib {
    from {
      image = "eclipse-temurin:25-alpine"
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
