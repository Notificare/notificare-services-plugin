plugins {
    id("com.gradle.plugin-publish")
}

val artifactChannel = if (version.toString().isStableVersion()) "releases" else "prereleases"

val properties = loadProperties(rootProject.file("local.properties"))
val accessKey: String? = System.getenv("AWS_ACCESS_KEY_ID") ?: properties.getProperty("aws.s3.access_key_id")
val secretKey: String? = System.getenv("AWS_SECRET_ACCESS_KEY") ?: properties.getProperty("aws.s3.secret_access_key")

if (accessKey != null && secretKey != null) {
    publishing {
        repositories {
            maven {
                name = "S3"
                url = uri("s3://maven.notifica.re/$artifactChannel")
                credentials(AwsCredentials::class) {
                    accessKey = accessKey
                    secretKey = secretKey
                }
            }
        }
    }
}
