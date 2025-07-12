pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Thêm các kho khác nếu cần, ví dụ: JitPack
        // maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "BaiCuoiKy04"
include(":app")