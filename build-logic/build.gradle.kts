plugins { `kotlin-dsl` }

gradlePlugin {
  plugins {
    create("passwordDictionaries") {
      id = "net.particify.arsnova.password-dictionaries"
      implementationClass = "net.particify.arsnova.buildlogic.PasswordDictionariesPlugin"
    }
  }
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:6.0.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
