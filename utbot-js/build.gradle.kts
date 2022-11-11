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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    api(project(":utbot-framework"))
    implementation(project(":utbot-fuzzers"))
    // https://mvnrepository.com/artifact/org.graalvm.js/js
    implementation(group = "org.graalvm.js", name = "js", version = "22.1.0.1")

    // https://mvnrepository.com/artifact/org.graalvm.js/js-scriptengine
    implementation(group = "org.graalvm.js", name = "js-scriptengine", version = "22.1.0.1")

    // https://mvnrepository.com/artifact/org.graalvm.truffle/truffle-api
    implementation(group = "org.graalvm.truffle", name = "truffle-api", version = "22.1.0.1")

    // https://mvnrepository.com/artifact/org.graalvm.sdk/graal-sdk
    implementation(group = "org.graalvm.sdk", name = "graal-sdk", version = "22.1.0.1")

    // https://mvnrepository.com/artifact/org.json/json
    implementation(group = "org.json", name = "json", version = "20220320")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation(group = "commons-io", name = "commons-io", version = "2.11.0")

    implementation("org.functionaljava:functionaljava:5.0")
    implementation("org.functionaljava:functionaljava-quickcheck:5.0")
    implementation("org.functionaljava:functionaljava-java-core:5.0")
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
}