subprojects {
  tasks.register<Copy>("getDeps") {
    from(project.the<SourceSetContainer>()["main"].runtimeClasspath)
    into(layout.buildDirectory.dir("get-deps"))

    doFirst {
      delete(layout.buildDirectory.dir("get-deps"))
    }

    doLast {
      delete(layout.buildDirectory.dir("get-deps"))
    }
  }
}
