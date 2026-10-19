pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pentalingo"

include(":app")
include(":core-script")
include(":core-model")
include(":core-engine")
include(":content")
include(":data")
include(":core-ui")
include(":feature-path")
include(":feature-lesson")
include(":feature-codex")
