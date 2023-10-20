val kotlinLoggingVersion: String by rootProject
val semVer: String? by rootProject
val slf4jVersion: String by rootProject

// === IDE settings ===
val projectType: String by rootProject
val communityEdition: String by rootProject
val ultimateEdition: String by rootProject

val ideType: String by rootProject
val androidStudioPath: String? by rootProject

val ideaVersion: String? by rootProject
val pycharmVersion: String? by rootProject
val goVersion: String? by rootProject

val javaIde: String? by rootProject
val pythonIde: String? by rootProject
val jsIde: String? by rootProject
val goIde: String? by rootProject

val ideVersion = when(ideType) {
    "PC", "PY" -> pycharmVersion
    "GO" -> goVersion
    else -> ideaVersion
}

val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject
val goPluginVersion: String? by rootProject

// https://plugins.jetbrains.com/docs/intellij/android-studio.html#configuring-the-plugin-pluginxml-file
val ideTypeOrAndroidStudio = if (androidStudioPath == null) ideType else "IC"

project.tasks.asMap["runIde"]?.enabled = false
// === IDE settings ===

plugins {
    id("org.jetbrains.intellij") version "1.13.1"
}

intellij {
    version.set(ideVersion)
    type.set(ideType)
}

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

    runIde {
        jvmArgs("-Xmx2048m")
        jvmArgs("--add-exports", "java.desktop/sun.awt.windows=ALL-UNNAMED")
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("232.*")
        version.set(semVer)
    }
}

dependencies {
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "org.jetbrains", name = "annotations", version = "16.0.2")
    implementation(project(":utbot-api"))
    implementation(project(":utbot-framework"))
    implementation(group = "org.slf4j", name = "slf4j-api", version = slf4jVersion)
}
