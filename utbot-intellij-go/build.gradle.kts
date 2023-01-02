val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject
val ideType: String? by rootProject
val ideVersion: String by rootProject
val kotlinPluginVersion: String by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject
val goPluginVersion: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.7.0"
}
project.tasks.asMap["runIde"]?.enabled = true

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
    implementation(project(":utbot-ui-commons"))

    //Family
    implementation(project(":utbot-go"))
}

intellij {

    val androidPlugins = listOf("org.jetbrains.android")

    val jvmPlugins = listOf(
        "java",
        "org.jetbrains.kotlin:222-1.7.20-release-201-IJ4167.29"
    )

    val pythonCommunityPlugins = listOf(
        "PythonCore:${pythonCommunityPluginVersion}"
    )

    val pythonUltimatePlugins = listOf(
        "Pythonid:${pythonUltimatePluginVersion}"
    )

    val jsPlugins = listOf(
        "JavaScript"
    )

    val goPlugins = listOf(
        "org.jetbrains.plugins.go:${goPluginVersion}"
    )

    plugins.set(
        when (ideType) {
            "IC" -> jvmPlugins + pythonCommunityPlugins + androidPlugins
            "IU" -> jvmPlugins + pythonUltimatePlugins + jsPlugins + goPlugins + androidPlugins
            "PC" -> pythonCommunityPlugins
            "PU" -> pythonUltimatePlugins // something else, JS?
            else -> jvmPlugins
        }
    )

    version.set(ideVersion)
    type.set(ideType)
}