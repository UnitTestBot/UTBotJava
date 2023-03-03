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

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }

    test {
        useJUnitPlatform()
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    api(project(":utbot-framework"))
    api(project(":utbot-fuzzers"))
    // https://mvnrepository.com/artifact/com.google.javascript/closure-compiler
    implementation("com.google.javascript:closure-compiler:v20221102")

    // https://mvnrepository.com/artifact/org.json/json
    implementation(group = "org.json", name = "json", version = "20220320")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation(group = "commons-io", name = "commons-io", version = "2.11.0")

    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation("org.functionaljava:functionaljava:5.0")
    implementation("org.functionaljava:functionaljava-quickcheck:5.0")
    implementation("org.functionaljava:functionaljava-java-core:5.0")
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
}
