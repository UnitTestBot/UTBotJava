import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

val rdVersion: String by rootProject
val log4j2Version: String by rootProject
val kotlinLoggingVersion: String? by rootProject

plugins {
    id("org.springframework.boot") version "2.7.8"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(project(":utbot-spring-analyzer-model"))
    implementation(project(":utbot-rd"))
    implementation(project(":utbot-core"))
    implementation("com.jetbrains.rd:rd-framework:$rdVersion")
    implementation("com.jetbrains.rd:rd-core:$rdVersion")
}

application {
    mainClass.set("org.utbot.spring.process.SpringAnalyzerProcessMainKt")
}

// see more details about this task -- https://github.com/spring-projects/spring-boot/issues/1828
tasks.shadowJar {
    isZip64 = true
    // Required for Spring
    mergeServiceFiles()
    append("META-INF/spring.handlers")
    append("META-INF/spring.schemas")
    append("META-INF/spring.tooling")
    transform(PropertiesFileTransformer().apply {
        paths = listOf("META-INF/spring.factories")
        mergeStrategy = "append"
    })

    transform(Log4j2PluginsCacheFileTransformer::class.java)
    archiveFileName.set("utbot-spring-analyzer-shadow.jar")
}

val springAnalyzerJar: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(springAnalyzerJar.name, tasks.shadowJar)
}
