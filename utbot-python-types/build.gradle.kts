val kotlinLoggingVersion: String? by rootProject

dependencies {
    implementation("com.squareup.moshi:moshi:1.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
    implementation("com.squareup.moshi:moshi-adapters:1.11.0")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = kotlinLoggingVersion)
}

val utbotMypyRunnerVersion = File(project.projectDir, "src/main/resources/utbot_mypy_runner_version").readText()
val pipToken: String? by project
val pythonInterpreter: String? by project
val utbotMypyRunnerPath = File(project.projectDir, "src/main/python/utbot_mypy_runner")
val localMypyPath = File(utbotMypyRunnerPath, "dist")
val localMypyPathText = File(project.projectDir, "src/main/resources/local_mypy_path")


val setMypyRunnerVersion = tasks.register<Exec>("setVersion") {
    group = "python"
    workingDir = utbotMypyRunnerPath
    commandLine(pythonInterpreter!!, "-m", "poetry", "version", utbotMypyRunnerVersion)
}

val buildMypyRunner = tasks.register<Exec>("buildUtbotMypyRunner") {
    dependsOn(setMypyRunnerVersion)
    group = "python"
    workingDir = utbotMypyRunnerPath
    commandLine(pythonInterpreter!!, "-m", "poetry", "build")
    localMypyPathText.writeText(localMypyPath.canonicalPath)
    localMypyPathText.createNewFile()
}

tasks.register<Exec>("publishUtbotMypyRunner") {
    dependsOn(buildMypyRunner)
    group = "python"
    workingDir = utbotMypyRunnerPath
    commandLine(pythonInterpreter!!, "-m", "poetry", "publish", "-u", "__token__", "-p", pipToken!!)
}