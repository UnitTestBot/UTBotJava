val kotlinLoggingVersion: String by rootProject
val rgxgenVersion: String by rootProject

dependencies {
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "com.github.curious-odd-man", name = "rgxgen", version = rgxgenVersion)
}

java {
    withSourcesJar()
}