import java.io.ByteArrayOutputStream
import java.io.PrintWriter

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
    type.set(ideTypeOrAndroidStudio)
}

abstract class SettingsToConfigTask : DefaultTask() {
    @get:Internal
    val settingsSourceDir = File(project.buildDir.parentFile.parentFile, "utbot-framework-api/src/main/kotlin/org/utbot/framework/")
    @get:Internal
    val sourceFileName = "UtSettings.kt"
    @get:Internal
    val settingsResourceDir = File(project.buildDir.parentFile.parentFile, "utbot-intellij/src/main/resources/")
    @get:Internal
    val settingsFileName = "settings.properties"

    @TaskAction
    fun proceed() {
        try {
            val constMap = mutableMapOf<String, String>()
            val acc = StringBuilder()
            val docLines = mutableListOf<String>()
            val byteArrayOutputStream = ByteArrayOutputStream()
            val writer = PrintWriter(byteArrayOutputStream)
            File(settingsSourceDir, sourceFileName).useLines {
                it.iterator().forEach { line ->
                    var s = line.trim()
                    if (s.startsWith("const val ")) {
                        val pos = s.indexOf(" = ")
                        constMap[s.substring(10, pos)] = s.substring(pos + 3)
                    } else if (s == "/**") {
                        docLines.clear()
                    } else if (s.startsWith("* ")) {
                        docLines.add(s.substring(2))
                    } else if (s.startsWith("var")) {
                        acc.clear()
                        acc.append(s)
                    } else if (s.isEmpty() && !acc.isEmpty()) {
                        s = acc.toString()
                        acc.clear()
                        if (s.startsWith("var")) {
                            var i = s.indexOf(" by ", 3)
                            if (i > 0) {
                                var propertyName = s.substring(3, i).trim()
                                if (propertyName.contains(':')) {
                                    propertyName = propertyName.substring(0, propertyName.lastIndexOf(':'))
                                }
                                s = s.substring(i + 7)
                                i = s.indexOf("Property")
                                if (i > 0) {
                                    val type = s.subSequence(0, i)
                                    i = s.indexOf('(', i)
                                    if (i > 0) {
                                        s = s.substring(i + 1)
                                        var defaultValue = s.substring(0, s.indexOf(')'))
                                        defaultValue = constMap[defaultValue] ?:defaultValue
                                        if (byteArrayOutputStream.size() > 0) {
                                            writer.println()
                                            writer.println("#")
                                        }
                                        for (docLine in docLines) {
                                            writer.println("# $docLine")
                                        }
                                        writer.println("$propertyName=$defaultValue")
                                        writer.flush()
                                    }
                                }
                            } else {
                                System.err.println(s)
                            }
                        }
                    } else if (acc.isNotEmpty()) {
                        acc.append(" $s")
                    }
                }
                writer.flush()
                writer.close()
                File(settingsResourceDir, settingsFileName).writeBytes(byteArrayOutputStream.toByteArray())
            }
        } catch (e : java.io.IOException) {
            logger.error("Unexpected error when processing $sourceFileName", e)
        }
    }
}

tasks.register<SettingsToConfigTask>("generateConfigTemplate")

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
}

dependencies {
    implementation(group ="com.jetbrains.rd", name = "rd-framework", version = "2022.3.1")
    implementation(group ="com.jetbrains.rd", name = "rd-core", version = "2022.3.1")
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
}