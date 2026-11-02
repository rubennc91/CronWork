pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "CronWork"
include(":core")
// -PcoreOnly lets the pure Kotlin module build where no Android SDK is available.
if (!providers.gradleProperty("coreOnly").isPresent) include(":app")
