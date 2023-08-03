import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

val springVersion: String by rootProject
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
    implementation(project(":utbot-core"))

    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot
    compileOnly("org.springframework.boot:spring-boot:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-test-autoconfigure:$springBootVersion")
    compileOnly("org.springframework:spring-test:$springVersion")
    compileOnly("org.springframework:spring-tx:$springVersion")
    compileOnly("org.springframework:spring-web:$springVersion")
    compileOnly("org.springframework.data:spring-data-commons:$springBootVersion")

    compileOnly("javax.persistence:javax.persistence-api:$javaxVersion")
    compileOnly("jakarta.persistence:jakarta.persistence-api:$jakartaVersion")

    implementation("com.jetbrains.rd:rd-core:$rdVersion") { exclude(group = "org.slf4j", module = "slf4j-api") }
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
