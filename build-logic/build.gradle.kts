plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.plugins.gradle.publish.asDependency())
}

fun Provider<PluginDependency>.asDependency(): Provider<String> =
    this.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
