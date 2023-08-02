val kotlinLoggingVersion: String by rootProject
val ideType: String by rootProject
val ideVersion: String by rootProject
val semVer: String? by rootProject
val slf4jVersion: String by rootProject
val androidStudioPath: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.13.1"
}
project.tasks.asMap["runIde"]?.enabled = false

intellij {
    version.set(ideVersion)
    type.set(ideType)

    plugins.set(listOf(
        "java",
        "org.jetbrains.android"
    ))
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
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
    implementation(group = "org.jetbrains", name = "annotations", version = "16.0.2")
    implementation(project(":utbot-api"))
    implementation(project(":utbot-framework"))
    implementation(group = "org.slf4j", name = "slf4j-api", version = slf4jVersion)
}
