val kotlinLoggingVersion: String by rootProject
val rdVersion: String by rootProject
val sootVersion: String by rootProject

val fetchSpringCommonsJar: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val fetchSpringAnalyzerJar: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    implementation(project(":utbot-framework"))
    implementation(project(":utbot-spring-commons-api"))
    implementation(project(":utbot-spring-analyzer"))

    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "com.jetbrains.rd", name = "rd-core", version = rdVersion)

    implementation("org.unittestbot.soot:soot-utbot-fork:${sootVersion}") {
        exclude(group = "com.google.guava", module = "guava")
    }

    fetchSpringCommonsJar(project(":utbot-spring-commons", configuration = "springCommonsJar"))
    fetchSpringAnalyzerJar(project(":utbot-spring-analyzer", configuration = "springAnalyzerJar"))
}

tasks.processResources {
    from(fetchSpringCommonsJar) {
        into("lib")
    }

    from(fetchSpringAnalyzerJar) {
        into("lib")
    }
}