val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject
val ideType: String? by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject

dependencies {
    api(project(":utbot-fuzzing"))
    api(project(":utbot-framework"))
    api(project(":utbot-python-parser"))
    api(project(":utbot-python-types"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.12.0")
    implementation(group = "commons-io", name = "commons-io", version = "2.11.0")
    implementation("com.squareup.moshi:moshi:1.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
    implementation("com.squareup.moshi:moshi-adapters:1.11.0")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation("org.functionaljava:functionaljava:5.0")
    implementation("org.functionaljava:functionaljava-quickcheck:5.0")
    implementation("org.functionaljava:functionaljava-java-core:5.0")
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
}