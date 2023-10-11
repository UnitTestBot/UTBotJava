val projectType: String by rootProject
val communityEdition: String by rootProject
val ultimateEdition: String by rootProject

val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject

val ideType: String? by rootProject
val ideVersion: String? by rootProject
val pythonCommunityPluginVersion: String? by rootProject
val pythonUltimatePluginVersion: String? by rootProject
val goPluginVersion: String? by rootProject

val sootVersion: String? by rootProject
val kryoVersion: String? by rootProject
val rdVersion: String? by rootProject
val semVer: String? by rootProject
val androidStudioPath: String? by rootProject

val junit5Version: String by rootProject
val junit4PlatformVersion: String by rootProject

// https://plugins.jetbrains.com/docs/intellij/android-studio.html#configuring-the-plugin-pluginxml-file
val ideTypeOrAndroidStudio = if (androidStudioPath == null) ideType else "IC"

project.tasks.asMap["runIde"]?.enabled = false

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

    val mavenUtilsPlugins = listOf(
        "org.jetbrains.idea.maven"
    )

    val basePluginSet = jvmPlugins + kotlinPlugins + mavenUtilsPlugins + androidPlugins

    plugins.set(basePluginSet)

    version.set(ideVersion)
    type.set(ideTypeOrAndroidStudio)
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
    implementation(group ="com.jetbrains.rd", name = "rd-framework", version = rdVersion)
    implementation(group ="com.jetbrains.rd", name = "rd-core", version = rdVersion)
    implementation(group ="com.esotericsoftware.kryo", name = "kryo5", version = kryoVersion)
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "org.apache.commons", name = "commons-text", version = apacheCommonsTextVersion)
    implementation("org.apache.httpcomponents.client5:httpclient5:5.1")
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)

    implementation(project(":utbot-framework")) { exclude(group = "org.slf4j", module = "slf4j-api") }
    implementation(project(":utbot-spring-framework")) { exclude(group = "org.slf4j", module = "slf4j-api") }
    implementation(project(":utbot-java-fuzzing"))
    //api(project(":utbot-analytics"))
    testImplementation("org.mock-server:mockserver-netty:5.4.1")
    testApi(project(":utbot-framework"))

    implementation(project(":utbot-ui-commons"))
    implementation(project(":utbot-android-studio"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junit4PlatformVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junit5Version")
}
