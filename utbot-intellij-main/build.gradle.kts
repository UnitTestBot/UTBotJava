val semVer: String? by rootProject
val junit5Version: String by rootProject
val junit4PlatformVersion: String by rootProject

// === IDE settings ===
val projectType: String by rootProject
val communityEdition: String by rootProject
val ultimateEdition: String by rootProject

val ideType: String by rootProject
val androidStudioPath: String? by rootProject

val ideaVersion: String? by rootProject
val pycharmVersion: String? by rootProject
val golandVersion: String? by rootProject

val javaIde: String? by rootProject
val pythonIde: String? by rootProject
val jsIde: String? by rootProject
val goIde: String? by rootProject

val ideVersion = when(ideType) {
    "PC", "PY" -> pycharmVersion
    "GO" -> golandVersion
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

    val androidPlugins = listOf("org.jetbrains.android")

    val jvmPlugins = mutableListOf(
        "java"
    )

    val kotlinPlugins = listOf(
        "org.jetbrains.kotlin"
    )

    androidStudioPath?.let { jvmPlugins += androidPlugins }

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

    val mavenUtilsPlugins = listOf(
        "org.jetbrains.idea.maven"
    )

    val basePluginSet = jvmPlugins + kotlinPlugins + mavenUtilsPlugins + androidPlugins

    plugins.set(
        when (projectType) {
            communityEdition -> basePluginSet + pythonCommunityPlugins
            ultimateEdition -> when (ideType) {
                "IC" -> basePluginSet + pythonCommunityPlugins
                "IU" -> basePluginSet + pythonUltimatePlugins + jsPlugins + goPlugins
                "PC" -> pythonCommunityPlugins
                "PY" -> pythonUltimatePlugins + jsPlugins
                "GO" -> goPlugins
                else -> basePluginSet
            }
            else -> basePluginSet
        }
    )

    version.set(ideVersion)
    type.set(ideTypeOrAndroidStudio)
    SettingsTemplateHelper.proceed(project)
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
        androidStudioPath?.let { ideDir.set(file(it)) }
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("232.*")
        version.set(semVer)
    }
}

dependencies {
    implementation(project(":utbot-ui-commons"))

    //Family

    if (javaIde?.split(',')?.contains(ideType) == true) {
        implementation(project(":utbot-intellij"))
    }

    if (pythonIde?.split(',')?.contains(ideType) == true) {
        implementation(project(":utbot-python"))
        implementation(project(":utbot-intellij-python"))
    }

    if (projectType == ultimateEdition) {
        if (jsIde?.split(',')?.contains(ideType) == true) {
            implementation(project(":utbot-js"))
            implementation(project(":utbot-intellij-js"))
        }

        if (goIde?.split(',')?.contains(ideType) == true) {
            implementation(project(":utbot-go"))
            implementation(project(":utbot-intellij-go"))
        }
    }

    implementation(project(":utbot-android-studio"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junit4PlatformVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junit5Version")
}
