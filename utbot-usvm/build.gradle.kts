val jacoDbVersion: String by rootProject
val usvmVersion: String by rootProject
val approximationsVersion: String by rootProject

val approximationsRepo = "com.github.UnitTestBot.java-stdlib-approximations"
val usvmRepo = "com.github.UnitTestBot.usvm"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val approximations: Configuration by configurations.creating {}
val usvmApproximationsApi: Configuration by configurations.creating {}
val usvmInstrumentationCollector: Configuration by configurations.creating {}
val usvmInstrumentationRunner: Configuration by configurations.creating {}

dependencies {
    implementation(project(":utbot-framework-api"))

    implementation(group = "org.jacodb", name = "jacodb-core", version = jacoDbVersion)
    implementation(group = "org.jacodb", name = "jacodb-analysis", version = jacoDbVersion)
    implementation(group = "org.jacodb", name = "jacodb-approximations", version = jacoDbVersion)

    implementation(group = usvmRepo, name = "usvm-core", version = usvmVersion)
    implementation(group = usvmRepo, name = "usvm-jvm", version = usvmVersion)
    implementation(group = usvmRepo, name = "usvm-jvm-api", version = usvmVersion)
    implementation(group = usvmRepo, name = "usvm-jvm-instrumentation", version = usvmVersion)
    implementation(group = usvmRepo, name = "usvm-jvm-instrumentation-collectors", version = usvmVersion)

    approximations("$approximationsRepo:approximations:$approximationsVersion")

    usvmApproximationsApi("$usvmRepo:usvm-jvm-api:$usvmVersion")
    usvmInstrumentationCollector("$usvmRepo:usvm-jvm-instrumentation-collectors:$usvmVersion")
    usvmInstrumentationRunner("$usvmRepo:usvm-jvm-instrumentation:$usvmVersion")
    usvmInstrumentationRunner("$usvmRepo:usvm-jvm-instrumentation-collectors:$usvmVersion")
}

// TODO replace with runner from usvm (unavailable due to huge jar size)
val usvmInstrumentationRunnerJarTask by tasks.register<Jar>("usvmInstrumentationRunnerJar") {
    archiveBaseName.set("usvm-jvm-instrumentation-runner")
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class" to "org.usvm.instrumentation.rd.InstrumentedProcessKt",
            "Premain-Class" to "org.usvm.instrumentation.agent.Agent",
            "Can-Retransform-Classes" to "true",
            "Can-Redefine-Classes" to "true"
        )
    }

    from(configurations.named("usvmInstrumentationRunner").get().map {
        if (it.isDirectory) it else zipTree(it)
    })
}

tasks.processResources {
    listOf(
        approximations,
        usvmApproximationsApi,
        usvmInstrumentationCollector,
        usvmInstrumentationRunnerJarTask,
    ).forEach { source ->
        from(source) {
            into("lib")
            rename {
                it.replace("-$usvmVersion", "")
                    .replace("-$approximationsVersion", "")
            }
        }
    }
}
