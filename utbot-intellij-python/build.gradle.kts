val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val junitJupiterVersion: String? by rootProject

val ideType: String? by rootProject
val ideVersion: String by rootProject
val kotlinPluginVersion: String by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.7.0"
}
project.tasks.asMap["runIde"]?.enabled = false

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
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitJupiterVersion)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitJupiterVersion)

    implementation(project(":utbot-ui-commons"))

    //Family
    implementation(project(":utbot-python"))
}

intellij {

    val androidPlugins = listOf("org.jetbrains.android")

    val jvmPlugins = listOf(
        "java",
        "org.jetbrains.kotlin:$kotlinPluginVersion"
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

    plugins.set(
        when (ideType) {
            "IC" -> jvmPlugins + pythonCommunityPlugins + androidPlugins
            "IU" -> jvmPlugins + pythonUltimatePlugins + jsPlugins + androidPlugins
            "PC" -> pythonCommunityPlugins
            "PY" -> pythonUltimatePlugins // something else, JS?
            else -> jvmPlugins
        }
    )

    version.set(ideVersion)
    type.set(ideType)
}