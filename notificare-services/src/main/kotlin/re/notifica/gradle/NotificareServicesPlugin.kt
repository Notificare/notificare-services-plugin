package re.notifica.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.DynamicFeatureAndroidComponentsExtension
import com.android.build.api.variant.GeneratesApk
import com.android.build.api.variant.Variant
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

@Suppress("unused")
class NotificareServicesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.info("Initializing Notificare Services Plugin")

        var pluginApplied = false

        project.pluginManager.withPlugin("com.android.application") {
            pluginApplied = true
            project.extensions.configure(ApplicationAndroidComponentsExtension::class.java) {
                it.registerSourceType(SOURCE_TYPE)
                it.onVariants { variant -> handleVariant(variant, project) }
            }
        }

        project.pluginManager.withPlugin("com.android.dynamic-feature") {
            pluginApplied = true
            project.extensions.configure(DynamicFeatureAndroidComponentsExtension::class.java) {
                it.registerSourceType(SOURCE_TYPE)
                it.onVariants { variant -> handleVariant(variant, project) }
            }
        }

        project.afterEvaluate {
            if (pluginApplied) {
                return@afterEvaluate
            }

            project.logger.error(
                "The notificare-services Gradle plugin needs to be applied on a project with " +
                    "com.android.application or com.android.dynamic-feature."
            )
        }
    }

    private fun <T> handleVariant(
        variant: T,
        project: Project
    ) where T : Variant, T : GeneratesApk {
        val task = project.tasks.register(
            "process${variant.name.capitalized()}NotificareServices",
            NotificareServicesTask::class.java
        ) {
            it.notificareServicesJsonFiles.set(
                getJsonFiles(
                    variant.buildType.orEmpty(),
                    variant.productFlavors.map { it.second },
                    project.projectDir
                )
            )
        }

        try {
            variant.sources
                .getByName(SOURCE_TYPE)
                .addStaticSourceDirectory(
                    project.layout.projectDirectory
                        .dir("src/${variant.name}/$SOURCE_TYPE")
                        .toString()
                )
        } catch (e: IllegalArgumentException) {
            // directory doesn't exist. ignore.
        }

        variant.sources.res?.addGeneratedSourceDirectory(
            task,
            NotificareServicesTask::outputDirectory
        )
    }

    companion object {
        const val SOURCE_TYPE = "notificare-services"
        const val JSON_FILE_NAME = "notificare-services.json"

        fun getJsonFiles(buildType: String, flavorNames: List<String>, root: File): List<File> {
            return getJsonLocations(buildType, flavorNames).map { root.resolve(it) }
        }

        fun getJsonLocations(buildType: String, flavorNames: List<String>): List<String> {
            val fileLocations = mutableListOf<String>()

            fileLocations.add("")
            fileLocations.add("src/$buildType")

            if (flavorNames.isNotEmpty()) {
                val flavorName = flavorNames.stream()
                    .reduce("") { a, b -> a + if (a.isEmpty()) b else b.capitalized() }

                fileLocations.add("src/$flavorName/$buildType")
                fileLocations.add("src/$buildType/$flavorName")
                fileLocations.add("src/$flavorName")
                fileLocations.add("src/" + flavorName + buildType.capitalized())

                var fileLocation = "src"
                for (flavor in flavorNames) {
                    fileLocation += "/$flavor"
                    fileLocations.add(fileLocation)
                    fileLocations.add("$fileLocation/$buildType")
                    fileLocations.add(fileLocation + buildType.capitalized())
                }
            }

            return fileLocations
                .distinct()
                .sortedByDescending { path -> path.count { it == '/' } }
                .map { location ->
                    if (location.isEmpty()) JSON_FILE_NAME
                    else "$location/$JSON_FILE_NAME"
                }
        }
    }
}
