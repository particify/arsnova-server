/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.buildlogic

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/** Converts the tarball of one zxcvbn-ts language package with [PasswordDictionaryPackage]. */
@CacheableTransform
abstract class PasswordDictionaryTransform : TransformAction<TransformParameters.None> {
  @get:InputArtifact
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  abstract val tarball: Provider<FileSystemLocation>

  override fun transform(outputs: TransformOutputs) {
    val file = tarball.get().asFile
    file.inputStream().use {
      PasswordDictionaryPackage.convert(it, file.name, outputs.dir(file.name.removeSuffix(".tgz")))
    }
  }
}
