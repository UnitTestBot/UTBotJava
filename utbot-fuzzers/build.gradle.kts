import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val sootVersion: String by rootProject
val kotlinLoggingVersion: String by rootProject
val rgxgenVersion: String by rootProject

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

tasks {
    withType<ShadowJar> {
        archiveClassifier.set(" ")
        minimize()
    }
}

dependencies {
    implementation(project(":utbot-framework-api"))

    implementation(group = "org.unittestbot.soot", name = "soot-utbot-fork", version = sootVersion) {
        exclude(group="com.google.guava", module="guava")
    }
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "com.github.curious-odd-man", name = "rgxgen", version = rgxgenVersion)
}

tasks {
    compileJava {
        options.compilerArgs = emptyList()
    }
}