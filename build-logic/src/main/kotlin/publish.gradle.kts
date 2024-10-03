plugins {
    id("com.gradle.plugin-publish")
}

val properties = loadProperties(rootProject.file("local.properties"))
val awsAccessKey: String? = System.getenv("AWS_ACCESS_KEY_ID") ?: properties.getProperty("aws.s3.access_key_id")
val awsSecretKey: String? = System.getenv("AWS_SECRET_ACCESS_KEY") ?: properties.getProperty("aws.s3.secret_access_key")

afterEvaluate {
    val artifactChannel = if (version.toString().isStableVersion()) "releases" else "prereleases"

    if (awsAccessKey != null && awsSecretKey != null) {
        publishing {
            repositories {
                maven {
                    name = "S3"
                    url = uri("s3://maven.notifica.re/$artifactChannel")
                    credentials(AwsCredentials::class) {
                        accessKey = awsAccessKey
                        secretKey = awsSecretKey
                    }
                }
            }
        }
    }
}
