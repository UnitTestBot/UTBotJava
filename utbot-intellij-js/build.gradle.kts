val intellijPluginVersion: String? by rootProject
val kotlinLoggingVersion: String? by rootProject
val apacheCommonsTextVersion: String? by rootProject
val jacksonVersion: String? by rootProject
val ideType: String? by rootProject
val ideVersion: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.13.1"
}
project.tasks.asMap["runIde"]?.enabled = false

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

    test {
        useJUnitPlatform()
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(project(":utbot-ui-commons"))
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)

    //Family
    implementation(project(":utbot-js"))
}

intellij {
    val jsPlugins = listOf(
        "JavaScript"
    )

    plugins.set(
        when (ideType) {
            "IU" -> jsPlugins
            else -> emptyList()
        }
    )

    version.set(ideVersion)
    type.set(ideType)
}