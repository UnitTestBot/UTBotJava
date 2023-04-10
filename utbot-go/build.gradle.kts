val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject
val ideType: String? by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class")
            allWarningsAsErrors = false
        }
    }

    test {
        useJUnitPlatform()
    }
}

dependencies {
    api(project(":utbot-java-fuzzing"))
    api(project(":utbot-framework"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.beust:klaxon:5.5")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
}