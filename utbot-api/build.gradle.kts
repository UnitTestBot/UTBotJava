import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java {
    withSourcesJar()
}

tasks {
    withType<ShadowJar> {
        archiveClassifier.set(" ")
        minimize()
    }
}