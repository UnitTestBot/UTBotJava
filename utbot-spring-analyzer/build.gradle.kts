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

val shadowConfiguration by configurations.creating {}

dependencies {
    fun shadowAndImplementation(dependencyNotation: Any) {
        shadowConfiguration(dependencyNotation)
        implementation(dependencyNotation)
    }

    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot
    implementation("org.springframework.boot:spring-boot:$springBootVersion")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    shadowAndImplementation(project(":utbot-rd"))
    shadowAndImplementation(project(":utbot-core"))
    shadowAndImplementation(project(":utbot-framework-api"))
    shadowAndImplementation("com.jetbrains.rd:rd-framework:$rdVersion")
    shadowAndImplementation("com.jetbrains.rd:rd-core:$rdVersion")
    shadowAndImplementation("commons-logging:commons-logging:$commonsLoggingVersion")
    shadowAndImplementation("commons-io:commons-io:$commonsIOVersion")
}

shadowConfiguration.exclude(group = "org.slf4j", module = "slf4j-api")

application {
    mainClass.set("org.utbot.spring.process.SpringAnalyzerProcessMainKt")
}

// see more details about this task -- https://github.com/spring-projects/spring-boot/issues/1828
tasks.shadowJar {
    this@shadowJar.configurations = listOf(shadowConfiguration)

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
