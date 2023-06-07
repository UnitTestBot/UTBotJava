import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

val asmVersion: String by rootProject
val kryoVersion: String by rootProject
val kryoSerializersVersion: String by rootProject
val kotlinLoggingVersion: String by rootProject
val rdVersion: String by rootProject
val springBootVersion: String by rootProject

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
    application
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("org.utbot.instrumentation.process.InstrumentedProcessMainKt")
}

val fetchSpringCommonsJar: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    implementation(project(":utbot-framework-api"))
    implementation(project(":utbot-rd"))
    implementation(project(":utbot-spring-commons-api"))


    implementation("org.ow2.asm:asm:$asmVersion")
    implementation("org.ow2.asm:asm-commons:$asmVersion")
    implementation("com.esotericsoftware.kryo:kryo5:$kryoVersion")
    // this is necessary for serialization of some collections
    implementation("de.javakaffee:kryo-serializers:$kryoSerializersVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    implementation("com.jetbrains.rd:rd-framework:$rdVersion")
    implementation("com.jetbrains.rd:rd-core:$rdVersion")
    implementation("net.java.dev.jna:jna-platform:5.5.0")

    // TODO: this is necessary for inline classes mocking in UtExecutionInstrumentation
    implementation("org.mockito:mockito-core:4.2.0")
    implementation("org.mockito:mockito-inline:4.2.0")

    compileOnly("org.springframework.boot:spring-boot:$springBootVersion")
    fetchSpringCommonsJar(project(":utbot-spring-commons", configuration = "springCommonsJar"))
}

/**
 * Shadow plugin unpacks the nested `utbot-spring-commons-shadow.jar`.
 * But we need it to be packed. Workaround: double-nest the jar.
 */
val shadowJarUnpackWorkaround by tasks.register<Jar>("shadowBugWorkaround") {
    destinationDirectory.set(layout.buildDirectory.dir("build/shadow-bug-workaround"))
    from(fetchSpringCommonsJar) {
        into("lib")
    }
}

tasks.shadowJar {
    dependsOn(shadowJarUnpackWorkaround)

    from(shadowJarUnpackWorkaround) {
        into("lib")
    }

    manifest {
        attributes(
            "Main-Class" to "org.utbot.instrumentation.process.InstrumentedProcessMainKt",
            "Premain-Class" to "org.utbot.instrumentation.agent.Agent",
        )
    }

    transform(Log4j2PluginsCacheFileTransformer::class.java)
    archiveFileName.set("utbot-instrumentation-shadow.jar")
}

val instrumentationArchive: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(instrumentationArchive.name, tasks.shadowJar)
}