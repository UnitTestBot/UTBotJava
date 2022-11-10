val intellijPluginVersion: String? by rootProject

val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val apacheCommonsLangVersion: String? by rootProject
val commonsIoVersion: String? by rootProject
val moshiVersion: String? by rootProject
val functionaljavaVersion: String? by rootProject

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

    test {
        useJUnitPlatform()
    }
}

dependencies {
    api(project(":utbot-fuzzers"))
    api(project(":utbot-framework"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(group = "commons-io", name = "commons-io", version = commonsIoVersion)
    implementation(group = "org.apache.commons", name = "commons-lang3", version = apacheCommonsLangVersion)
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)

    api(group = "org.functionaljava", name = "functionaljava", version = functionaljavaVersion)
    api(group = "org.functionaljava", name = "functionaljava-quickcheck", version = functionaljavaVersion)
    api(group = "org.functionaljava", name = "functionaljava-java-core", version = functionaljavaVersion)

    implementation(group = "io.github.danielnaczo", name = "python3parser", version = "1.0.4") // TODO: will be changed to javacc21
    implementation("com.beust:klaxon:5.5") // TODO: remove this dependency and use only moshi

    implementation(group = "com.squareup.moshi", name = "moshi", version = moshiVersion)
    implementation(group = "com.squareup.moshi", name = "moshi-kotlin", version = moshiVersion)
    implementation(group = "com.squareup.moshi", name = "moshi-adapters", version = moshiVersion)
}

repositories {
    mavenCentral()
}
