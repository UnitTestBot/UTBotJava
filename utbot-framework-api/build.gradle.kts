import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junit4Version: String by rootProject
val sootCommitHash: String by rootProject
val commonsLangVersion: String by rootProject
val kotlinLoggingVersion: String? by rootProject

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    api(project(":utbot-core"))
    api(project(":utbot-api"))
    api(project(":utbot-rd"))
    implementation(group ="com.jetbrains.rd", name = "rd-framework", version = "2022.3.1")
    implementation(group ="com.jetbrains.rd", name = "rd-core", version = "2022.3.1")
    implementation("com.github.UnitTestBot:soot:${sootCommitHash}")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    // TODO do we really need apache commons?
    implementation(group = "org.apache.commons", name = "commons-lang3", version = commonsLangVersion)
    testImplementation(group = "junit", name = "junit", version = junit4Version)
}

tasks {
    withType<ShadowJar> {
        archiveClassifier.set(" ")
        minimize()
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
}