import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat

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
    val apacheLines =
"""Copyright (c) ${SimpleDateFormat("yyyy").format(System.currentTimeMillis())} The UnitTestBot Authors

Licensed under the Apache License, Version 2.0 (the \License\);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an \AS IS\ BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.""".split("\n")
    @get:Internal
    val settingsSourceDir = File(project.buildDir.parentFile.parentFile, "utbot-framework-api/src/main/kotlin/org/utbot/framework/")
    @get:Internal
    val sourceFileName = "UtSettings.kt"
    @get:Internal
    val settingsResourceDir = File(project.buildDir.parentFile.parentFile, "utbot-intellij/src/main/resources/")
    @get:Internal
    val settingsFileName = "settings.properties"

    data class PropertyModel(
        val key: String,
        var type: String = "",
        var defaultValue: String = "",
        var docLines: MutableList<String> = mutableListOf()
    )

    data class EnumInfo(var className: String, var docMap: MutableMap<String, MutableList<String>> = linkedMapOf())

    @TaskAction
    fun proceed() {
        try {
            val dictionary = mutableMapOf<String, String>().also {
                it["Int.MAX_VALUE"] = Int.MAX_VALUE.toString()
            }
            val models = mutableListOf<PropertyModel>()
            val enums = mutableListOf<EnumInfo>()

            val acc = StringBuilder()
            val docLines = mutableListOf<String>()
            File(settingsSourceDir, sourceFileName).useLines { it ->
                it.iterator().forEach { line ->
                    var s = line.trim()
                    if (s.startsWith("enum class ")) {
                        enums.add(EnumInfo(s.substring(11, s.length - 2)))
                    } else if (s.matches(Regex("[A-Z_]+,?")) && enums.isNotEmpty()) {
                        var enumValue = s.substring(0, s.length)
                        if (enumValue.endsWith(",")) enumValue = enumValue.substring(0, enumValue.length - 1)
                        enums.last().docMap[enumValue] = docLines.toMutableList()
                    } else if (s.startsWith("const val ")) {
                        val pos = s.indexOf(" = ")
                        dictionary[s.substring(10, pos)] = s.substring(pos + 3)
                    } else if (s == "/**") {
                        docLines.clear()
                    } else if (s.startsWith("* ")) {
                        if (!s.contains("href")) {//Links are not supported
                            docLines.add(s.substring(2))
                        }
                    } else if (s.startsWith("var") || s.startsWith("val")) {
                        acc.clear()
                        acc.append(s)
                    } else if (s.isEmpty() && acc.isNotEmpty()) {
                        s = acc.toString()
                        acc.clear()
                        if (s.startsWith("var") || s.startsWith("val")) {
                            var i = s.indexOf(" by ", 3)
                            if (i > 0) {
                                var key = s.substring(3, i).trim()
                                println(key)
                                if (key.contains(':')) {
                                    key = key.substring(0, key.lastIndexOf(':'))
                                }
                                val model = PropertyModel(key)
                                models.add(model)
                                s = s.substring(i + 7)
                                i = s.indexOf("Property")
                                if (i > 0) model.type = s.substring(0, i)
                                if (i == 0) {
                                    i = s.indexOf('<', i)
                                    if (i != -1) {
                                        s = s.substring(i+1)
                                        i = s.indexOf('>')
                                        if (i != -1) {
                                            model.type = s.substring(0, i)
                                        }
                                    }
                                }

                                    i = s.indexOf('(', i)
                                    if (i > 0) {
                                        s = s.substring(i + 1)
                                        var defaultValue = s.substring(0, s.indexOf(')')).trim()
                                        if (defaultValue.contains(',')) defaultValue = defaultValue.substring(0, defaultValue.indexOf(','))
                                        defaultValue = dictionary[defaultValue] ?:defaultValue
                                        if (defaultValue.matches(Regex("[\\d_]+L"))) {
                                            defaultValue = defaultValue.substring(0, defaultValue.length - 1).replace("_", "")
                                        }
                                        if (defaultValue.matches(Regex("^\".+\"$"))) {
                                            defaultValue = defaultValue.substring(1, defaultValue.length - 1)
                                        }
                                        model.defaultValue = defaultValue
                                        model.docLines.addAll(docLines)
                                    }
                            } else {
                                System.err.println(s)
                            }
                        }
                    } else if (acc.isNotEmpty()) {
                        acc.append(" $s")
                    }
                }
                val byteArrayOutputStream = ByteArrayOutputStream()
                val writer = PrintWriter(byteArrayOutputStream)
                apacheLines.forEach { writer.println("# $it") }

                for (model in models) {
                    if (model.type == "Enum") {
                        val split = model.defaultValue.split('.')
                        if (split.size > 1) {
                            model.defaultValue = split[1]
                            val enumInfo = enums.find { info -> info.className == split[0] }
                            if (enumInfo!= null) {
                                model.docLines.add("")
                            }
                            enumInfo?.docMap?.forEach {
                                if (it.value.size == 1) {
                                    model.docLines.add("${it.key}: ${it.value.first()}")
                                } else {
                                    model.docLines.add(it.key)
                                    it.value.forEach { line -> model.docLines.add(line) }
                                }
                            }
                        }
                    }
                    writer.println()
                    writer.println("#")
                    for (docLine in model.docLines) {
                        if (docLine.isEmpty()) {
                            writer.println("#")
                        } else {
                            writer.println("# $docLine")
                        }
                    }
                    if (!model.docLines.any({ s -> s.toLowerCaseAsciiOnly().contains("default") })) {
                        writer.println("#")
                        writer.println("# Default value is [${model.defaultValue}]")
                    }
                    writer.println("${model.key}=${model.defaultValue}")
                    writer.flush()
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