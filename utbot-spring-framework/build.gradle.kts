val kotlinLoggingVersion: String by rootProject
val sootVersion: String by rootProject

val fetchSpringCommonsJar: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    implementation(project(":utbot-framework"))
    implementation(project(":utbot-spring-commons-api"))
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation("org.unittestbot.soot:soot-utbot-fork:${sootVersion}") {
        exclude(group = "com.google.guava", module = "guava")
    }

    fetchSpringCommonsJar(project(":utbot-spring-commons", configuration = "springCommonsJar"))
}

tasks.processResources {
    from(fetchSpringCommonsJar) {
        into("lib")
    }
}