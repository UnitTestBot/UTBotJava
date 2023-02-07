val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject

val ideType: String? by rootProject
val ideVersion: String? by rootProject
val kotlinPluginVersion: String? by rootProject

val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject

val pythonIde: String? by rootProject
val jsIde: String? by rootProject

val sootVersion: String? by rootProject
val kryoVersion: String? by rootProject
val rdVersion: String? by rootProject
val semVer: String? by rootProject
val androidStudioPath: String? by rootProject

// https://plugins.jetbrains.com/docs/intellij/android-studio.html#configuring-the-plugin-pluginxml-file
val ideTypeOrAndroidStudio = if (androidStudioPath == null) ideType else "IC"

plugins {
    id("org.jetbrains.intellij") version "1.7.0"
}

intellij {

    val androidPlugins = listOf("org.jetbrains.android")

    val jvmPlugins = mutableListOf(
        "java",
        "org.jetbrains.kotlin:$kotlinPluginVersion"
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

    val mavenUtilsPlugins = listOf(
        "org.jetbrains.idea.maven"
    )

    plugins.set(
        when (ideType) {
            "IC" -> jvmPlugins + pythonCommunityPlugins + androidPlugins + mavenUtilsPlugins
            "IU" -> jvmPlugins + pythonUltimatePlugins + jsPlugins + androidPlugins + mavenUtilsPlugins
            "PC" -> pythonCommunityPlugins
            "PY" -> pythonUltimatePlugins // something else, JS?
            else -> jvmPlugins
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
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class")
            allWarningsAsErrors = false
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_11
    }

    runIde {
        jvmArgs("-Xmx2048m")
        jvmArgs("--add-exports", "java.desktop/sun.awt.windows=ALL-UNNAMED")
        androidStudioPath?.let { ideDir.set(file(it)) }
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("222.*")
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
    implementation(group ="com.jetbrains.rd", name = "rd-framework", version = rdVersion)
    implementation(group ="com.jetbrains.rd", name = "rd-core", version = rdVersion)
    implementation(group ="com.esotericsoftware.kryo", name = "kryo5", version = kryoVersion)
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
    implementation("org.apache.httpcomponents.client5:httpclient5:5.1")
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)

    implementation(project(":utbot-framework")) { exclude(group = "org.slf4j", module = "slf4j-api") }
    implementation(project(":utbot-fuzzers"))
    //api(project(":utbot-analytics"))
    testImplementation("org.mock-server:mockserver-netty:5.4.1")
    testApi(project(":utbot-framework"))

    implementation(project(":utbot-ui-commons"))

    //Family
    if (pythonIde?.split(',')?.contains(ideType) == true) {
        implementation(project(":utbot-python"))
        implementation(project(":utbot-intellij-python"))
    }

    if (jsIde?.split(',')?.contains(ideType) == true) {
        implementation(project(":utbot-js"))
        implementation(project(":utbot-intellij-js"))
    }

    implementation(project(":utbot-android-studio"))
    implementation(project(":utbot-spring-analyzer"))

    testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
    testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")

    testImplementation("org.assertj:assertj-core:3.11.1")

    // Logging Network Calls
    testImplementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Video Recording
    implementation("com.automation-remarks:video-recorder-junit5:2.0")
}