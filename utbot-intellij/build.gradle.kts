val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject
val ideType: String? by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject
val sootCommitHash: String? by rootProject
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
        "org.jetbrains.kotlin:222-1.7.20-release-201-IJ4167.29"
    )

    androidStudioPath?.let { jvmPlugins += androidPlugins }

    val pythonCommunityPlugins = listOf(
        "PythonCore:${pythonCommunityPluginVersion}"
    )

    val pythonUltimatePlugins = listOf(
        "Pythonid:${pythonUltimatePluginVersion}"
    )

    val jsPlugins = listOf(
        "JavaScriptLanguage"
    )

    plugins.set(
        when (ideType) {
            "IC" -> jvmPlugins + pythonCommunityPlugins + androidPlugins
            "IU" -> jvmPlugins + pythonUltimatePlugins + jsPlugins + androidPlugins
            "PC" -> pythonCommunityPlugins
            "PU" -> pythonUltimatePlugins // something else, JS?
            else -> jvmPlugins
        }
    )

    version.set("222.4167.29")
    type.set(ideTypeOrAndroidStudio)
}

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
//    implementation("com.github.UnitTestBot:soot:${sootCommitHash}")
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

    implementation(project(":utbot-android-studio"))
}