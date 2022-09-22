import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kotlinLoggingVersion: String by rootProject
val junit4Version: String by rootProject

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "net.java.dev.jna", name = "jna-platform", version = "5.5.0")

    testImplementation(group = "junit", name = "junit", version = junit4Version)
}

tasks {
    withType<ShadowJar> {
        archiveClassifier.set(" ")
        minimize()
    }
}