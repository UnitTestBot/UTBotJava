plugins {
    id("org.jetbrains.intellij") version "1.13.1"
}

intellij {
    /*
        The list of Android Studio releases can be found here https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
        For each release a compatible Intellij Idea version can be found in the right column. Specify it in "version.set("...")

        NOTE!!!
        We use Android Studio Chipmunk (2021.2.1), although Android Studio Dolphin (2021.3.1) has been released.
        The reason is that a version of Kotlin plugin compatible with Android Studio is required.
        The list of Kotlin plugin releases can be found here https://plugins.jetbrains.com/plugin/6954-kotlin/versions/stable
        The last compatible with AS plugin version on 19 Oct 2022 is Kotlin 212-1.7.10-release-333-AS5457.46,
        it is not compatible with Dolphin release (https://plugins.jetbrains.com/plugin/6954-kotlin/versions/stable/193255).
     */

    val androidPlugins = listOf("org.jetbrains.android")

    val jvmPlugins = listOf(
        "java",
        "org.jetbrains.kotlin:212-1.7.10-release-333-AS5457.46"
    )

    plugins.set(jvmPlugins + androidPlugins)

    version.set("212.5712.43")
    type.set("IC")
}

project.tasks.asMap["runIde"]?.enabled = false

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class")
            allWarningsAsErrors = false
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}