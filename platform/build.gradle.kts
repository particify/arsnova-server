plugins {
  `java-platform`
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
  constraints {
    versionCatalog.libraryAliases.forEach { alias ->
      api(versionCatalog.findLibrary(alias).get())
    }
  }
}
