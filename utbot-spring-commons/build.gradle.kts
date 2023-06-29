import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

val springBootVersion: String by rootProject
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
    compileOnly("org.springframework:spring-test:5.2.6.RELEASE")                    // TODO: fix the version
    compileOnly("org.springframework:spring-tx:5.3.9")                              // TODO: fix the version
    compileOnly("org.springframework.boot:spring-boot-test-autoconfigure:2.7.0")    // TODO: fix the version
    compileOnly("org.springframework.data:spring-data-commons:$springBootVersion")
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
