import org.apache.tools.ant.taskdefs.condition.Os

val semVer: String? by rootProject
val rdVersion: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.13.1"
}

intellij {
    type.set("RD")
    version.set("2023.1-SNAPSHOT")
}

dependencies {
    implementation(group ="com.jetbrains.rd", name = "rd-framework", version = rdVersion)
    implementation(group ="com.jetbrains.rd", name = "rd-core", version = rdVersion)
}

tasks {
    val dotNetSdkCmdPath = projectDir.resolve("dotnet-sdk.cmd").toString()

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class")
            allWarningsAsErrors = false
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }

    runIde {
        jvmArgs("-Xmx2048m")
    }

    patchPluginXml {
        sinceBuild.set("231")
        version.set(semVer)
    }

    buildSearchableOptions {
        enabled = false
    }

    val chmodDotnet = create("chmodDotnet") {
        group = "build"
        doLast {
            exec {
                commandLine = listOf(
                    "chmod",
                    "+x",
                    dotNetSdkCmdPath
                )
            }
        }
    }

    val publishDotNet = create("publishDotNet") {
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            dependsOn(chmodDotnet)
        }
        group = "build"
        doLast {
            exec {
                commandLine = listOf(
                    dotNetSdkCmdPath,
                    "publish",
                    projectDir.resolve("src/dotnet/UtBot/UtBot.sln").toString()
                )
            }
        }
    }

    prepareSandbox {
        dependsOn(publishDotNet)
        from("src/dotnet/UtBot/UtBot/bin/Debug/net6.0/publish") {
            into("${intellij.pluginName.get()}/dotnet") }
    }

}
