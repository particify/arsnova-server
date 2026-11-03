/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.buildlogic

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Collects the converted packages into the resources, with the list of available languages and the
 * notice the packages require on redistribution.
 */
@CacheableTask
abstract class PasswordDictionaryResources : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val packages: ConfigurableFileCollection

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @get:Inject abstract val fileSystem: FileSystemOperations

  @TaskAction
  fun assemble() {
    val output = outputDirectory.get().asFile
    fileSystem.sync {
      from(packages) { include("dictionaries/**") }
      into(File(output, "password"))
    }
    val roots = packages.files.sortedBy { it.name }
    val languages =
        roots
            .flatMap { File(it, "dictionaries").list()?.toList() ?: listOf() }
            .filter { it != "common" }
            .sorted()
    File(output, "password/dictionaries/languages.txt")
        .writeText(languages.joinToString("") { "$it\n" })
    File(output, "password/NOTICE.md").writeText(notice(roots))
  }

  private fun notice(roots: List<File>): String {
    val names = roots.map { File(it, "notices/package.txt").readText() }
    val licenses = roots.map { File(it, "notices/LICENSE.txt").readText().trim() }.toSet()
    if (licenses.size != 1) {
      throw GradleException("The zxcvbn-ts packages' license texts differ")
    }
    val attributions =
        roots
            .mapIndexedNotNull { i, root ->
              File(root, "notices/attribution.md")
                  .takeIf { it.exists() }
                  ?.let { it.readText() to names[i] }
            }
            .groupBy({ it.first }, { it.second })
    val sections =
        attributions.entries.joinToString("") { (text, packages) ->
          "## Data attribution\n\nApplies to ${packages.joinToString(", ")}.\n\n$text\n\n"
        }
    return "# Third-party data\n\n" +
        "The word lists in dictionaries/ are the dictionaries of these packages:\n\n" +
        names.joinToString("") { "- $it\n" } +
        "\nThe layouts in keyboards/ are those of zxcvbn-ts as well.\n" +
        "All of them are licensed as follows.\n\n" +
        licenses.single() +
        "\n\n" +
        sections.trimEnd() +
        "\n"
  }
}
