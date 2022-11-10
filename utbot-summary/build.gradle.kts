val kotlinLoggingVersion: String by rootProject
val junit4Version: String by rootProject
val junit5Version: String by rootProject
val sootVersion: String by rootProject
val mockitoVersion: String by rootProject
val haifenglSmileVersion: String by rootProject
val javaparserVersion: String by rootProject

dependencies {
    implementation(project(":utbot-framework-api"))
    implementation(group = "org.unittestbot.soot", name = "soot-utbot-fork", version = sootVersion) {
        exclude(group="com.google.guava", module="guava")
    }
    implementation(project(":utbot-fuzzers"))
    implementation(project(":utbot-instrumentation"))

    implementation(group = "com.github.haifengl", name = "smile-kotlin", version = haifenglSmileVersion)
    implementation(group = "com.github.haifengl", name = "smile-core", version = haifenglSmileVersion)
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "com.github.javaparser", name = "javaparser-core", version = javaparserVersion)
    testImplementation(group = "org.mockito", name = "mockito-core", version = mockitoVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = junit5Version)
}
