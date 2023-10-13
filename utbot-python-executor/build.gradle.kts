val kotlinLoggingVersion: String? by rootProject

val utbotExecutorVersion = File(project.projectDir, "src/main/resources/utbot_executor_version").readText()
// these two properties --- from GRADLE_USER_HOME/gradle.properties
val pypiToken: String? by project
val pythonInterpreter: String? by project
val utbotExecutorPath = File(project.projectDir, "src/main/python/utbot_executor")
val localUtbotExecutorPath = File(utbotExecutorPath, "dist")

tasks.register("cleanDist") {
    group = "python"
    delete(localUtbotExecutorPath.canonicalPath)
}

val installPoetry =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("installPoetry") {
            group = "python"
            workingDir = utbotExecutorPath
            commandLine(pythonInterpreter, "-m", "pip", "install", "poetry")
        }
    } else {
        null
    }

val setExecutorVersion =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("setVersion") {
            dependsOn(installPoetry!!)
            group = "python"
            workingDir = utbotExecutorPath
            commandLine(pythonInterpreter, "-m", "poetry", "version", utbotExecutorVersion)
        }
    } else {
        null
    }

val buildExecutor =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("buildUtbotExecutor") {
            dependsOn(setExecutorVersion!!)
            group = "python"
            workingDir = utbotExecutorPath
            commandLine(pythonInterpreter, "-m", "poetry", "build")
        }
    } else {
        null
    }

if (pythonInterpreter != null && pypiToken != null) {
    tasks.register<Exec>("publishUtbotExecutor") {
        dependsOn(buildExecutor!!)
        group = "python"
        workingDir = utbotExecutorPath
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

val installExecutor =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("installUtbotExecutor") {
            dependsOn(buildExecutor!!)
            group = "python"
            environment("PIP_FIND_LINKS" to localUtbotExecutorPath.canonicalPath)
            commandLine(
                pythonInterpreter,
                "-m",
                "pip",
                "install",
                "utbot_executor==$utbotExecutorVersion"
            )
        }
    } else {
        null
    }

val installPytest =
    if (pythonInterpreter != null) {
        tasks.register<Exec>("installPytest") {
            group = "pytest"
            workingDir = utbotExecutorPath
            commandLine(pythonInterpreter, "-m", "pip", "install", "pytest")
        }
    } else {
        null
    }

if (pythonInterpreter != null) {
    tasks.register<Exec>("runTests") {
        dependsOn(installExecutor!!)
        dependsOn(installPytest!!)
        group = "pytest"
        workingDir = utbotExecutorPath
        commandLine(
            pythonInterpreter,
            "-m",
            "pytest",
        )
    }
}
