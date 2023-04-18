import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
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

val withoutSpringConfiguration by configurations.creating {}
val withSpringConfiguration by configurations.creating {
    extendsFrom(withoutSpringConfiguration)
}
configurations.implementation.get().extendsFrom(withSpringConfiguration)

dependencies {
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot
    withSpringConfiguration("org.springframework.boot:spring-boot:$springBootVersion")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    fun ModuleDependency.excludeSlf4jApi() = exclude(group = "org.slf4j", module = "slf4j-api")

    withoutSpringConfiguration(project(":utbot-rd")) { excludeSlf4jApi() }
    withoutSpringConfiguration(project(":utbot-core")) { excludeSlf4jApi() }
    withoutSpringConfiguration(project(":utbot-framework-api")) { excludeSlf4jApi() }
    withoutSpringConfiguration("com.jetbrains.rd:rd-framework:$rdVersion") { excludeSlf4jApi() }
    withoutSpringConfiguration("com.jetbrains.rd:rd-core:$rdVersion") { excludeSlf4jApi() }
    withoutSpringConfiguration("commons-logging:commons-logging:$commonsLoggingVersion") { excludeSlf4jApi() }
    withoutSpringConfiguration("commons-io:commons-io:$commonsIOVersion") { excludeSlf4jApi() }
}

application {
    mainClass.set("org.utbot.spring.process.SpringAnalyzerProcessMainKt")
}

val shadowWithoutSpring by tasks.register<ShadowJar>("shadowJarWithoutSpring") {
    configureShadowJar(withoutSpringConfiguration)
    archiveFileName.set("utbot-spring-analyzer-shadow.jar")
}

val shadowWithSpring by tasks.register<ShadowJar>("shadowJarWithSpring") {
    configureShadowJar(withSpringConfiguration)
    archiveFileName.set("utbot-spring-analyzer-with-spring-shadow.jar")
}

val springAnalyzerJar: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(springAnalyzerJar.name, shadowWithoutSpring)
    add(springAnalyzerJar.name, shadowWithSpring)
}

fun ShadowJar.configureShadowJar(configuration: Configuration) {
    // see more details -- https://github.com/johnrengelman/shadow/blob/master/src/main/groovy/com/github/jengelman/gradle/plugins/shadow/ShadowJavaPlugin.groovy
    group = "shadow"
    from(sourceSets.main.get().output)
    exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")

    configurations = listOf(configuration)

    // see more details -- https://github.com/spring-projects/spring-boot/issues/1828
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
}
