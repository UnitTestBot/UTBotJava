val mockitoVersion: String by rootProject
val kotlinLoggingVersion: String by rootProject
val apacheCommonsTextVersion: String by rootProject

dependencies {
    api(project(":utbot-framework-api"))
    implementation(group="org.mockito", name="mockito-core", version=mockitoVersion)
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.12.0")
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
    implementation("org.functionaljava:functionaljava:5.0")
    implementation("org.functionaljava:functionaljava-quickcheck:5.0")
    implementation("org.functionaljava:functionaljava-java-core:5.0")
}