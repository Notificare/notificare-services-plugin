pluginManagement {
    repositories {
        maven {
            val pluginRepo: String by settings
            url = uri(pluginRepo)
        }

        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        // Centralized version declarations. These do not directly impact the classpath. Rather,
        // this simply lets you have a single place to declare all plugin versions.

        val agpVersion: String by settings
        id("com.android.application") version agpVersion

        val pluginVersion: String by settings
        id("re.notifica.gradle.notificare-services") version pluginVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")
