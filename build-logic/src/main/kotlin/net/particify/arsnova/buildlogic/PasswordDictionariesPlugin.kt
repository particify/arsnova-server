/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer

/** Holds the zxcvbn-ts packages, each declared with an artifact of type [NPM_TARBALL_TYPE]. */
const val PASSWORD_DICTIONARY_PACKAGES = "passwordDictionaryPackages"
const val NPM_TARBALL_TYPE = "tgz"
private const val PASSWORD_DICTIONARY_TYPE = "password-dictionary"

/**
 * Generates the zxcvbn-ts word lists the password strength estimate uses besides zxcvbn4j's own
 * into the main resources. Each npm package declared in [PASSWORD_DICTIONARY_PACKAGES] is converted
 * into rank-ordered list files plus its part of the license and attribution notice.
 */
class PasswordDictionariesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    addNpmRepository(project)
    val packages =
        project.configurations.create(PASSWORD_DICTIONARY_PACKAGES) {
          isCanBeConsumed = false
          isCanBeResolved = true
        }
    project.dependencies.registerTransform(PasswordDictionaryTransform::class.java) {
      from.attribute(ARTIFACT_TYPE_ATTRIBUTE, NPM_TARBALL_TYPE)
      to.attribute(ARTIFACT_TYPE_ATTRIBUTE, PASSWORD_DICTIONARY_TYPE)
    }
    val resources =
        project.tasks.register(
            "passwordDictionaryResources", PasswordDictionaryResources::class.java) {
              this.packages.from(
                  packages.incoming
                      .artifactView {
                        attributes { attribute(ARTIFACT_TYPE_ATTRIBUTE, PASSWORD_DICTIONARY_TYPE) }
                      }
                      .files)
              outputDirectory.set(
                  project.layout.buildDirectory.dir("generated/password-dictionaries"))
            }
    project.plugins.withType(JavaPlugin::class.java) {
      project.extensions.getByType(SourceSetContainer::class.java).named("main") {
        this.resources.srcDir(resources)
      }
    }
  }

  /** The packages are published to npm only, so the repository serves them and nothing else. */
  private fun addNpmRepository(project: Project) {
    project.repositories.exclusiveContent {
      forRepository {
        project.repositories.ivy {
          name = "npm"
          setUrl("https://registry.npmjs.org/")
          patternLayout { artifact("[organisation]/[module]/-/[module]-[revision].[ext]") }
          metadataSources { artifact() }
        }
      }
      filter { includeGroup("@zxcvbn-ts") }
    }
  }
}
