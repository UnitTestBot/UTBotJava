import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

val springVersion: String by rootProject
val springSecurityVersion: String by rootProject
val springBootVersion: String by rootProject
val javaxVersion: String by rootProject
val jakartaVersion: String by rootProject
val rdVersion: String by rootProject

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":utbot-spring-commons-api"))


    // these dependencies are compilieOnly, because they should
    // already be present on the classpath in all utbot processes
    compileOnly(project(":utbot-core"))
    compileOnly("com.jetbrains.rd:rd-core:$rdVersion") { exclude(group = "org.slf4j", module = "slf4j-api") }


    // these dependencies are compilieOnly, because they should
    // be picked up from user classpath if they are present there
    compileOnly("org.springframework.boot:spring-boot:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-test-autoconfigure:$springBootVersion")
    compileOnly("org.springframework:spring-test:$springVersion")
    compileOnly("org.springframework:spring-tx:$springVersion")
    compileOnly("org.springframework:spring-web:$springVersion")
    compileOnly("org.springframework.security:spring-security-test:$springSecurityVersion")
    compileOnly("org.springframework.data:spring-data-commons:$springBootVersion")

    compileOnly("javax.persistence:javax.persistence-api:$javaxVersion")
    compileOnly("jakarta.persistence:jakarta.persistence-api:$jakartaVersion")
}

tasks.shadowJar {
    isZip64 = true

    transform(Log4j2PluginsCacheFileTransformer::class.java)
    archiveFileName.set("utbot-spring-commons-shadow.jar")
}

val springCommonsJar: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(springCommonsJar.name, tasks.shadowJar)
}
