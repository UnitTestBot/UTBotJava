import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

val springBootVersion: String by rootProject
val rdVersion: String by rootProject
val commonsLoggingVersion: String by rootProject
val kotlinLoggingVersion: String by rootProject
val commonsIOVersion: String by rootProject

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val shadowJarConfiguration: Configuration by configurations.creating {}
configurations.implementation.get().extendsFrom(shadowJarConfiguration)

dependencies {
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot
    compileOnly("org.springframework.boot:spring-boot:$springBootVersion")
    compileOnly("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    compileOnly("commons-io:commons-io:$commonsIOVersion")

    fun ModuleDependency.excludeSlf4jApi() = exclude(group = "org.slf4j", module = "slf4j-api")

    // TODO stop putting dependencies that are only used in SpringAnalyzerProcess into shadow jar
    implementation(project(":utbot-rd")) { excludeSlf4jApi() }
    implementation(project(":utbot-core")) { excludeSlf4jApi() }
    implementation(project(":utbot-framework-api")) { excludeSlf4jApi() }
    implementation("com.jetbrains.rd:rd-framework:$rdVersion") { excludeSlf4jApi() }
    implementation("com.jetbrains.rd:rd-core:$rdVersion") { excludeSlf4jApi() }
    implementation("commons-logging:commons-logging:$commonsLoggingVersion") { excludeSlf4jApi() }
}

application {
    mainClass.set("org.utbot.spring.process.SpringAnalyzerProcessMainKt")
}

// see more details -- https://github.com/spring-projects/spring-boot/issues/1828
tasks.shadowJar {
    isZip64 = true

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
