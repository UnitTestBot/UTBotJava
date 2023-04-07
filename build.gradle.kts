import java.text.SimpleDateFormat
import org.gradle.api.JavaVersion.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.utbot"

val kotlinVersion: String by project
val semVer: String? by project
val coroutinesVersion: String by project
val collectionsVersion: String by project
val junit5Version: String by project
val dateBasedVersion: String = SimpleDateFormat("YYYY.MM").format(System.currentTimeMillis()) // CI proceeds the same way

version = semVer ?: "$dateBasedVersion-SNAPSHOT"

plugins {
    `java-library`
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

allprojects {

    apply {
        plugin("maven-publish")
        plugin("kotlin")
    }

    tasks {
        withType<JavaCompile> {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
            options.encoding = "UTF-8"
            options.compilerArgs = options.compilerArgs + "-Xlint:all"
        }
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class", "-Xcontext-receivers")
                allWarningsAsErrors = false
            }
        }
        compileTestKotlin {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs + listOf("-Xallow-result-return-type", "-Xsam-conversions=class", "-Xcontext-receivers")
                allWarningsAsErrors = false
            }
        }
        withType<Test> {
            // uncomment if you want to see loggers output in console
            // this is useful if you debug in docker
            // testLogging.showStandardStreams = true
            // testLogging.showStackTraces = true

            // set heap size for the test JVM(s)
            minHeapSize = "128m"
            maxHeapSize = "3072m"
            jvmArgs = listOf(
                "-XX:MaxHeapSize=3072m",
                "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens", "java.base/java.util.concurrent.locks=ALL-UNNAMED",
                "--add-opens", "java.base/java.text=ALL-UNNAMED",
                "--add-opens", "java.base/java.io=ALL-UNNAMED",
                "--add-opens", "java.base/java.nio=ALL-UNNAMED",
                "--add-opens", "java.base/java.nio.file=ALL-UNNAMED",
                "--add-opens", "java.base/java.net=ALL-UNNAMED",
                "--add-opens", "java.base/sun.security.util=ALL-UNNAMED",
                "--add-opens", "java.base/sun.reflect.generics.repository=ALL-UNNAMED",
                "--add-opens", "java.base/sun.net.util=ALL-UNNAMED",
                "--add-opens", "java.base/sun.net.fs=ALL-UNNAMED",
                "--add-opens", "java.base/java.security=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.ref=ALL-UNNAMED",
                "--add-opens", "java.base/java.math=ALL-UNNAMED",
                "--add-opens", "java.base/java.util.stream=ALL-UNNAMED",
                "--add-opens", "java.base/java.util=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens", "java.base/sun.security.provider=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.event=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.jimage=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.jimage.decompressor=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.jmod=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.jtrfs=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.logger=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.math=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.module=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.org.objectweb.asm.commons=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.org.objectweb.asm.signature=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.org.objectweb.asm.tree.analysis=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.org.objectweb.asm.util=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.org.xml.sax=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.org.xml.sax.helpers=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.perf=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.platform=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.ref=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.reflect=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.util=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.util.jar=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.util.xml=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.util.xml.impl=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.vm=ALL-UNNAMED",
                "--add-opens", "java.base/jdk.internal.vm.annotation=ALL-UNNAMED"
            )

            useJUnitPlatform {
                excludeTags = setOf("slow", "IntegrationTest")
            }

            addTestListener(object : TestListener {
                override fun beforeSuite(suite: TestDescriptor) {}
                override fun beforeTest(testDescriptor: TestDescriptor) {}
                override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
                    println("[$testDescriptor.classDisplayName] [$testDescriptor.displayName]: $result.resultType, length - ${(result.endTime - result.startTime) / 1000.0} sec")
                    if (result.resultType == TestResult.ResultType.FAILURE) {
                        println("Exception: " + result.exception?.stackTraceToString())
                    }
                }

                override fun afterSuite(testDescriptor: TestDescriptor, result: TestResult) {
                    if (testDescriptor.parent == null) { // will match the outermost suite
                        println("Test summary: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)")
                    }
                }
            })
        }
    }

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/orgunittestbotsoot-1004/")
        maven("https://plugins.gradle.org/m2")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/maven-central")
    }

    dependencies {
        implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = coroutinesVersion)
        implementation(
            group = "org.jetbrains.kotlinx",
            name = "kotlinx-collections-immutable-jvm",
            version = collectionsVersion
        )
        implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = kotlinVersion)
        implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect", version = kotlinVersion)

        testImplementation("org.junit.jupiter:junit-jupiter") {
            version {
                strictly(junit5Version)
            }
        }
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    publishing {
        publications {
            create<MavenPublication>("jar") {
                from(components["java"])
                groupId = "org.utbot"
                artifactId = project.name
            }
        }
    }
}

dependencies {
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = kotlinVersion)
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-allopen", version = kotlinVersion)
}

configure(
    listOf(
        project(":utbot-api"),
        project(":utbot-core"),
        project(":utbot-framework"),
        project(":utbot-framework-api"),
        project(":utbot-fuzzers"),
        project(":utbot-instrumentation"),
        project(":utbot-rd"),
        project(":utbot-summary")
    )
) {
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/UnitTestBot/UTBotJava")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
