val kotlinLoggingVersion: String by rootProject
val rgxgenVersion: String by rootProject

dependencies {
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "org.cornutum.regexp", name = "regexp-gen", version = "2.0.1")
}