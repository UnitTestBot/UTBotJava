import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

tasks {
    withType<ShadowJar> {
        archiveClassifier.set(" ")
        minimize()
    }
}

val sootCommitHash: String by rootProject
val kotlinLoggingVersion: String by rootProject
val rgxgenVersion: String by rootProject

dependencies {
    implementation(project(":utbot-framework-api"))

    implementation("com.github.UnitTestBot:soot:${sootCommitHash}")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "com.github.curious-odd-man", name = "rgxgen", version = rgxgenVersion)
}

tasks {
    compileJava {
        options.compilerArgs = emptyList()
    }
}