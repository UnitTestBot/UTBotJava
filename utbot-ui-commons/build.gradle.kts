val kotlinLoggingVersion: String by rootProject
val ideType: String by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.7.0"
}
project.tasks.asMap["runIde"]?.enabled = false

intellij {
    version.set("212.5712.43")
    type.set(ideType)

    plugins.set(listOf(
        "java",
        "org.jetbrains.kotlin:212-1.7.10-release-333-IJ5457.46",
        "org.jetbrains.android"
    ))
}

dependencies {
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "org.jetbrains", name = "annotations", version = "16.0.2")
    implementation(project(":utbot-api"))
    implementation(project(":utbot-framework"))
    implementation(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")
    api("com.jetbrains.intellij.idea:ideaIC:212.5712.43")
}
