// Plugins go on the root buildscript classpath so every module shares one copy.
// With -PcoreOnly the Android plugins are skipped (no Android SDK/Google Maven needed).
buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
    dependencies {
        classpath(libs.gradle.kotlin)
        if (!providers.gradleProperty("coreOnly").isPresent) {
            classpath(libs.gradle.android)
            classpath(libs.gradle.compose)
            classpath(libs.gradle.ksp)
        }
    }
}
