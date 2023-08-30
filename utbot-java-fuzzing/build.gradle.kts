val sootVersion: String by rootProject
val kotlinLoggingVersion: String by rootProject
val rgxgenVersion: String by rootProject
val guavaVersion: String by rootProject

dependencies {
    implementation(project(":utbot-framework-api"))
    api(project(":utbot-fuzzing"))
    api(project(":utbot-modificators-analyzer"))

    implementation("org.unittestbot.soot:soot-utbot-fork:${sootVersion}") {
        exclude(group="com.google.guava", module="guava")
    }
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "com.github.curious-odd-man", name = "rgxgen", version = rgxgenVersion)
    implementation(group = "com.google.guava", name = "guava", version = guavaVersion)
}