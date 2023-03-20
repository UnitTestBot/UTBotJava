import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

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
}

application {
    mainClass.set("org.utbot.spring.ApplicationRunnerKt")
}

tasks.withType(ShadowJar::class.java) {
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
}
