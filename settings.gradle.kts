pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.pkg.github.com/recloudstream/cloudstream")
        mavenCentral()
        google()
    }
}

rootProject.name = "CloudStreamExtensions"

// Provider modules - add new providers here
include(":ExampleProvider")
