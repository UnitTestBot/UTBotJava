import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junit4Version: String by rootProject
val sootVersion: String by rootProject
val commonsLangVersion: String by rootProject
val kotlinLoggingVersion: String? by rootProject
val rdVersion: String? by rootProject

java {
    withSourcesJar()
}

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    api(project(":utbot-core"))
    api(project(":utbot-api"))
    api(project(":utbot-rd"))
    implementation(group ="com.jetbrains.rd", name = "rd-framework", version = rdVersion)
    implementation(group ="com.jetbrains.rd", name = "rd-core", version = rdVersion)
    implementation("org.unittestbot.soot:soot-utbot-fork:${sootVersion}") {
        exclude(group="com.google.guava", module="guava")
    }
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