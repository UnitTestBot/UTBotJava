val kotlinLoggingVersion: String? by rootProject

dependencies {
    implementation("com.squareup.moshi:moshi:1.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
    implementation("com.squareup.moshi:moshi-adapters:1.11.0")
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

val setMypyRunnerVersion =
    if (pythonInterpreter != null)
        tasks.register<Exec>("setVersion") {
        group = "python"
        workingDir = utbotMypyRunnerPath
        commandLine(pythonInterpreter, "-m", "poetry", "version", utbotMypyRunnerVersion)
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
        dependsOn(buildMypyRunner)
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