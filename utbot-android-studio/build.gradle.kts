val ideType: String? by rootProject
val androidStudioPath: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.7.0"
}

project.tasks.asMap["runIde"]?.enabled = false

// https://plugins.jetbrains.com/docs/intellij/android-studio.html#configuring-the-plugin-pluginxml-file
val ideTypeOrAndroidStudio = if (androidStudioPath == null) ideType else "IC"

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class")
            allWarningsAsErrors = false
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


intellij {

    val androidPlugins = listOf("org.jetbrains.android")

    val jvmPlugins = listOf(
        "java",
        "org.jetbrains.kotlin:212-1.7.10-release-333-AS5457.46"
    )

    plugins.set(jvmPlugins + androidPlugins)

    version.set("212.5712.43")
    type.set(ideTypeOrAndroidStudio)
}