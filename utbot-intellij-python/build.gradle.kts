val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject
val ideType: String? by rootProject
val ideVersion: String by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject
val goPluginVersion: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.13.1"
}
project.tasks.asMap["runIde"]?.enabled = false

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class")
            allWarningsAsErrors = false
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    test {
        useJUnitPlatform()
    }
}

dependencies {
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(project(":utbot-ui-commons"))

    //Family
    implementation(project(":utbot-python"))
}

intellij {
    val jvmPlugins = listOf(
        "java"
    )

    val kotlinPlugins = mutableListOf(
        "org.jetbrains.kotlin"
    )

    val pythonCommunityPlugins = listOf(
        "PythonCore:${pythonCommunityPluginVersion}"
    )

    val pythonUltimatePlugins = listOf(
        "Pythonid:${pythonUltimatePluginVersion}"
    )

    plugins.set(
        when (ideType) {
            "IC" -> jvmPlugins + kotlinPlugins + pythonCommunityPlugins
            "IU" -> jvmPlugins + kotlinPlugins + pythonUltimatePlugins
            "PC" -> pythonCommunityPlugins
            "PY" -> pythonUltimatePlugins // something else, JS?
            else -> jvmPlugins
        }
    )

    version.set(ideVersion)
    type.set(ideType)
    SettingsTemplateHelper.proceed(project)
}