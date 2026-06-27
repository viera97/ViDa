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

rootProject.name = "ViDa"
include(":app", ":core", ":domain", ":data", ":feature-home", ":feature-expense", ":feature-expense-list", ":feature-category-management", ":feature-card-management", ":feature-income", ":feature-income-list", ":feature-stash-management", ":feature-recurring-expense-management", ":feature-rate-management", ":feature-transfer-management", ":feature-wallet-management")
