import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

val rdVersion: String by rootProject
val commonsLoggingVersion: String by rootProject
val kotlinLoggingVersion: String by rootProject

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot
    implementation("org.springframework.boot:spring-boot:2.7.8")

    implementation(project(":utbot-rd"))
    implementation(project(":utbot-core"))
    implementation(project(":utbot-framework-api"))
    implementation("com.jetbrains.rd:rd-framework:$rdVersion")
    implementation("com.jetbrains.rd:rd-core:$rdVersion")
    implementation("commons-logging:commons-logging:$commonsLoggingVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("commons-io:commons-io:2.11.0")
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
