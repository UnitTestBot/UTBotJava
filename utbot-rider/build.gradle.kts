import org.apache.tools.ant.taskdefs.condition.Os

val semVer: String? by rootProject
val rdVersion: String? by rootProject

plugins {
    id("org.jetbrains.intellij") version "1.7.0"
}

intellij {
    type.set("RD")
    version.set("2022.2")
}

dependencies {
    implementation(group ="com.jetbrains.rd", name = "rd-framework", version = rdVersion)
    implementation(group ="com.jetbrains.rd", name = "rd-core", version = rdVersion)
}

tasks {
    val dotNetSdkCmdPath = projectDir.resolve("dotnet-sdk.cmd").toString()

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
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("222.*")
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
