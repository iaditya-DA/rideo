pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // ✅ Add JitPack (optional for plugin dependencies)
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // ✅ This is the important one — JitPack for libraries
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "rideo"
include(":app")
