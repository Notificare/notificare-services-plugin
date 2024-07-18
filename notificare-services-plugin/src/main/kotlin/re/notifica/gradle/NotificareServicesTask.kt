package re.notifica.gradle

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

private typealias ResValuesMap = Map<String, Any>
private typealias MutableResValuesMap = MutableMap<String, Any>

@CacheableTask
abstract class NotificareServicesTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    val intermediateDir: File
        get() = outputDirectory.asFile.get()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val notificareServicesJsonFiles: Property<Collection<File>>

    @Throws(GradleException::class)
    @TaskAction
    fun action() {
        val searchedLocations = notificareServicesJsonFiles.get()
        val jsonFiles = searchedLocations.filter { it.isFile }

        if (jsonFiles.isEmpty()) {
            val message = """
                File ${NotificareServicesPlugin.JSON_FILE_NAME} is missing.
                The Notificare Services plugin cannot function without it.
                Searched locations: ${searchedLocations.joinToString { it.absolutePath }}
            """.trimIndent()

            throw GradleException(message)
        }

        val servicesFile = jsonFiles.first()
        logger.warn("Parsing json file: $servicesFile")

        // Clean up previous directories.
        intermediateDir.deleteRecursively()

        if (!intermediateDir.mkdirs()) {
            throw GradleException("Failed to create folder: $intermediateDir")
        }

        val values = File(intermediateDir, "values")
        if (!values.exists() && !values.mkdirs()) {
            throw GradleException("Failed to create folder: $values")
        }

        val resValues = parseJsonFileContents(servicesFile)
        File(values, "values.xml").writeText(getValuesContent(resValues), Charsets.UTF_8)
    }

    private fun parseJsonFileContents(file: File): ResValuesMap {
        val resValues: MutableResValuesMap = LinkedHashMap()

        val root = JsonParser.parseReader(file.bufferedReader())
        if (!root.isJsonObject) {
            throw GradleException("Malformed root json at ${file.absolutePath}")
        }

        parseProjectInfo(root.asJsonObject, resValues)
        parseHostsInfo(root.asJsonObject, resValues)

        return resValues
    }

    private fun parseProjectInfo(root: JsonObject, resValues: MutableResValuesMap) {
        val projectInfo = root.getAsJsonObject("project_info")
            ?: throw GradleException("Missing project_info object.")

        val applicationId = projectInfo.getAsJsonPrimitive("application_id")?.asString
            ?: throw GradleException("Missing project_info/application_id object.")

        val applicationKey = projectInfo.getAsJsonPrimitive("application_key")?.asString
            ?: throw GradleException("Missing project_info/application_key object.")

        val applicationSecret = projectInfo.getAsJsonPrimitive("application_secret")?.asString
            ?: throw GradleException("Missing project_info/application_secret object.")

        resValues["notificare_services_application_id"] = applicationId
        resValues["notificare_services_application_key"] = applicationKey
        resValues["notificare_services_application_secret"] = applicationSecret
    }

    private fun parseHostsInfo(root: JsonObject, resValues: MutableResValuesMap) {
        val hostsInfo = root.getAsJsonObject("hosts_info")
            ?: return

        val restApi = hostsInfo.getAsJsonPrimitive("rest_api")?.asString
            ?: throw GradleException("Missing hosts_info/rest_api object.")

        val appLinks = hostsInfo.getAsJsonPrimitive("app_links")?.asString
            ?: throw GradleException("Missing hosts_info/app_links object.")

        val shortLinks = hostsInfo.getAsJsonPrimitive("short_links")?.asString
            ?: throw GradleException("Missing hosts_info/short_links object.")

        resValues["notificare_services_hosts_rest_api"] = restApi
        resValues["notificare_services_hosts_app_links"] = appLinks
        resValues["notificare_services_hosts_short_links"] = shortLinks
    }

    private fun getValuesContent(values: ResValuesMap): String {
        val sb = StringBuilder(256)

        sb.append(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>

            """.trimIndent()
        )

        for ((name, value) in values) {
            when (value) {
                is String -> sb.append("    <string name=\"$name\" translatable=\"false\">$value</string>\n")
                is Boolean -> sb.append("    <bool name=\"$name\">$value</bool>\n")
                else -> {
                    logger.warn("Unsupported property '$name' with type '${value.javaClass.simpleName}'.")
                    continue
                }
            }
        }

        sb.append("</resources>\n")

        return sb.toString()
    }
}
