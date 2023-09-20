val projectType: String by rootProject
val communityEdition: String by rootProject
val ultimateEdition: String by rootProject

val ideType: String? by rootProject
val ideVersion: String? by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject
val goPluginVersion: String? by rootProject

val javaIde: String? by rootProject
val pythonIde: String? by rootProject
val jsIde: String? by rootProject
val goIde: String? by rootProject

val semVer: String? by rootProject
val androidStudioPath: String? by rootProject

val junit5Version: String by rootProject
val junit4PlatformVersion: String by rootProject

// https://plugins.jetbrains.com/docs/intellij/android-studio.html#configuring-the-plugin-pluginxml-file
val ideTypeOrAndroidStudio = if (androidStudioPath == null) ideType else "IC"

plugins {
    id("org.jetbrains.intellij") version "1.13.1"
}

intellij {

    val androidPlugins = listOf("org.jetbrains.android")

    val jvmPlugins = mutableListOf(
        "java"
    )

    val kotlinPlugins = mutableListOf(
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
                "PY" -> pythonUltimatePlugins // something else, JS?
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

val remoteRobotVersion = "0.11.16"

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

    runIdeForUiTests {
        jvmArgs("-Xmx2048m", "-Didea.is.internal=true", "-Didea.ui.debug.mode=true")

        systemProperty("robot-server.port", "8082") // default port 8580
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
        systemProperty("idea.trust.all.projects", "true")
        systemProperty("ide.mac.file.chooser.native", "false")
        systemProperty("jbScreenMenuBar.enabled", "false")
        systemProperty("apple.laf.useScreenMenuBar", "false")
        systemProperty("ide.show.tips.on.startup.default.value", "false")
    }

    downloadRobotServerPlugin {
        version.set(remoteRobotVersion)
    }

    test {
        description = "Runs UI integration tests."
        useJUnitPlatform {
            exclude("/org/utbot/**") //Comment this line to run the tests locally
        }
    }
}

repositories {
    maven("https://jitpack.io")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
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

    testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
    testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")

    testImplementation("org.assertj:assertj-core:3.11.1")

    // Logging Network Calls
    testImplementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Video Recording
    implementation("com.automation-remarks:video-recorder-junit5:2.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junit4PlatformVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junit5Version")
}
