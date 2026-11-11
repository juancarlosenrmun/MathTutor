pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "mathtutor"

include(
    "mathtutor-common",
    "mathtutor-persistence",
    "mathtutor-auth",
    "mathtutor-cas",
    "mathtutor-llm",
    "mathtutor-graphics",
    "mathtutor-tutor",
    "mathtutor-application"
)