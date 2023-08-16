val sootVersion: String by rootProject

dependencies {
    api(project(":utbot-framework-api"))

    implementation("org.unittestbot.soot:soot-utbot-fork:${sootVersion}") {
        exclude(group="com.google.guava", module="guava")
    }
}