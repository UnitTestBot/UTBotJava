import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
}

val springBootVersion: String by rootProject

dependencies {
    implementation("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")

    implementation(group = "org.springframework.boot", name = "spring-boot-starter-web", version = springBootVersion)
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa", version = springBootVersion)
    implementation(group = "org.springframework.boot", name = "spring-boot-starter-test", version = springBootVersion)
}

tasks.shadowJar {
    isZip64 = true

    transform(Log4j2PluginsCacheFileTransformer::class.java)
    archiveFileName.set("utbot-spring-sample-shadow.jar")

    // Required for Spring to run properly when using shadowJar
    // More details: https://github.com/spring-projects/spring-boot/issues/1828
    mergeServiceFiles()
    append("META-INF/spring.handlers")
    append("META-INF/spring.schemas")
    append("META-INF/spring.tooling")
    transform(PropertiesFileTransformer().apply {
        paths = listOf("META-INF/spring.factories")
        mergeStrategy = "append"
    })
}

val springSampleJar: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(springSampleJar.name, tasks.shadowJar)
}