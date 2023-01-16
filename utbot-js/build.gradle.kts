val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val commonsIoVersion: String? by rootProject
val jacksonVersion: String? by rootProject
val functionaljavaVersion: String? by rootProject
val junitJupiterVersion: String? by rootProject
val jsonVersion: String? by rootProject

val ideType: String? by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class")
            allWarningsAsErrors = false
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_11
    }

    test {
        useJUnitPlatform()
    }
}

dependencies {
    api(project(":utbot-framework"))
    implementation(project(":utbot-fuzzers"))

    // https://mvnrepository.com/artifact/com.google.javascript/closure-compiler
    implementation("com.google.javascript:closure-compiler:v20221102")

    // https://mvnrepository.com/artifact/org.json/json
    implementation(group = "org.json", name = "json", version = jsonVersion)

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation(group = "commons-io", name = "commons-io", version = commonsIoVersion)
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
}
