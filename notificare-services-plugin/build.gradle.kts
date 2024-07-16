plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publish)
}

group = "re.notifica.gradle"
version = libs.versions.notificare.services.get()

dependencies {
    compileOnly(libs.android.gradle.api)
    implementation(libs.google.gson)

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly(libs.android.gradle.api)
}

gradlePlugin {
    website = "https://notificare.com/"
    vcsUrl = "https://github.com/Notificare/notificare-services-plugin/"

    plugins {
        create("notificareServicesPlugin") {
            id = "re.notifica.gradle.notificare-services"
            implementationClass = "re.notifica.gradle.NotificareServicesPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo").map { it.asFile.path })
        }
    }
}

tasks.withType<Test>().configureEach {
    dependsOn("publishAllPublicationsToMavenRepository")

    systemProperty("pluginRepo", layout.buildDirectory.dir("repo").get().asFile.absolutePath)
    systemProperty("pluginVersion", version)
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
