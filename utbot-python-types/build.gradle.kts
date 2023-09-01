val kotlinLoggingVersion: String? by rootProject

dependencies {
    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("com.squareup.moshi:moshi-adapters:1.14.0")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
}

val utbotMypyRunnerVersion = File(project.projectDir, "src/main/resources/utbot_mypy_runner_version").readText()
// these two properties --- from GRADLE_USER_HOME/gradle.properties
val pypiToken: String? by project
val pythonInterpreter: String? by project
val utbotMypyRunnerPath = File(project.projectDir, "src/main/python/utbot_mypy_runner")
val localMypyPath = File(utbotMypyRunnerPath, "dist")

tasks.register("cleanDist") {
    group = "python"
    delete(localMypyPath.canonicalPath)
}

val installPoetry =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("installPoetry") {
            group = "python"
            workingDir = utbotMypyRunnerPath
            commandLine(pythonInterpreter, "-m", "pip", "install", "poetry")
        }
    } else {
        null
    }

val setMypyRunnerVersion =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("setVersion") {
            dependsOn(installPoetry!!)
            group = "python"
            workingDir = utbotMypyRunnerPath
            commandLine(pythonInterpreter, "-m", "poetry", "version", utbotMypyRunnerVersion)
        }
    } else {
        null
    }

val buildMypyRunner =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("buildUtbotMypyRunner") {
            dependsOn(setMypyRunnerVersion!!)
            group = "python"
            workingDir = utbotMypyRunnerPath
            commandLine(pythonInterpreter, "-m", "poetry", "build")
        }
    } else {
        null
    }

if (pythonInterpreter != null && pypiToken != null) {
    tasks.register<Exec>("publishUtbotMypyRunner") {
        dependsOn(buildMypyRunner!!)
        group = "python"
        workingDir = utbotMypyRunnerPath
        commandLine(
            pythonInterpreter,
            "-m",
            "poetry",
            "publish",
            "-u",
            "__token__",
            "-p",
            pypiToken
        )
    }
}

val uninstallMypyRunner =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("uninstallMypyRunner") {
            group = "python"
            commandLine(
                pythonInterpreter,
                "-m",
                "pip",
                "uninstall",
                "utbot_mypy_runner"
            )
            commandLine(
                pythonInterpreter,
                "-m",
                "pip",
                "cache",
                "purge"
            )
        }
    } else {
        null
    }

val installMypyRunner =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("installUtbotMypyRunner") {
            dependsOn(buildMypyRunner!!)
            group = "python"
            environment("PIP_FIND_LINKS" to localMypyPath.canonicalPath)
            commandLine(
                pythonInterpreter,
                "-m",
                "pip",
                "install",
                "utbot_mypy_runner==$utbotMypyRunnerVersion"
            )
        }
    } else {
        null
    }

val samplesDir = File(project.projectDir, "src/test/resources/samples")
val buildDir = File(project.buildDir, "samples_builds")
val jsonDir = File(project.projectDir, "src/test/resources")

fun getParamsForSample(sampleName: String): List<String> =
    listOf(
        sampleName,
        File(samplesDir, "$sampleName.py").canonicalPath,
        sampleName,
        buildDir.canonicalPath,
        File(jsonDir, "$sampleName.json").canonicalPath
    )

val samplesInfo = listOf(
    getParamsForSample("annotation_tests"),
    getParamsForSample("boruvka"),
    getParamsForSample("import_test"),
    getParamsForSample("subtypes"),
)

if (pythonInterpreter != null) {
    val subtasks = samplesInfo.map { params ->
        tasks.register<JavaExec>("regenerateJsonForTests_${params.first()}") {
            dependsOn(installMypyRunner!!)
            group = "python_subtasks"
            classpath = sourceSets.test.get().runtimeClasspath
            args = listOf(pythonInterpreter) + params.drop(1)
            mainClass.set("org.utbot.python.newtyping.samples.GenerateMypyInfoBuildKt")
        }
    }

    tasks.register("regenerateJsonForTests") {
        subtasks.forEach { dependsOn(it) }
        group = "python"
    }
}