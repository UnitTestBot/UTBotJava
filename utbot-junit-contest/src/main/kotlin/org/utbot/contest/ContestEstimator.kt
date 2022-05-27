package org.utbot.contest

import mu.KotlinLogging
import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.common.FileUtil
import org.utbot.common.bracket
import org.utbot.common.info
import org.utbot.common.pid
import org.utbot.contest.Paths.classesLists
import org.utbot.contest.Paths.dependenciesJars
import org.utbot.contest.Paths.evosuiteGeneratedTestsFile
import org.utbot.contest.Paths.evosuiteJarPath
import org.utbot.contest.Paths.evosuiteOutputDir
import org.utbot.contest.Paths.evosuiteReportFile
import org.utbot.contest.Paths.jarsDir
import org.utbot.contest.Paths.moduleTestDir
import org.utbot.contest.Paths.outputDir
import org.utbot.features.FeatureExtractorFactoryImpl
import org.utbot.features.FeatureProcessorWithStatesRepetitionFactory
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.ConcreteExecutor
import java.io.File
import java.io.FileInputStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

private val classPathSeparator = System.getProperty("path.separator")
private val javaHome = if (System.getProperty("user.name") == "d00555580") {
    "C:/Program Files/Java/jdk1.8.0_241"
} else {
    System.getenv("JAVA_HOME")
}
private val javacCmd = "$javaHome/bin/javac"
private val javaCmd = "$javaHome/bin/java"

fun Array<String>.toText() = joinToString(separator = ",")

@Suppress("unused")
object ContestEstimator

object Paths {
    private const val projectPath = "utbot-junit-contest"
    private const val resourcesPath = "$projectPath/src/main/resources"
    private const val evosuiteDir = "$resourcesPath/evosuite"

    const val classesLists = "$resourcesPath/classes"
    const val jarsDir = "$resourcesPath/projects"
    const val outputDir = "$projectPath/build/output"
    const val moduleTestDir = "$projectPath/src/test/java"
    const val evosuiteJarPath = "$evosuiteDir/evosuite-1.2.0.jar"
    const val evosuiteOutputDir = "$outputDir/evosuite"
    private const val evosuiteReportPath = "$evosuiteOutputDir/evosuite-report"
    private const val evosuiteGeneratedTestsPath = "$evosuiteOutputDir/evosuite-tests"

    val evosuiteReportFile = File(evosuiteReportPath)
    val evosuiteGeneratedTestsFile = File(evosuiteGeneratedTestsPath)

    private val dependenciesJarsDir = System.getProperty("utBotDependenciesJarsDir") ?: "$resourcesPath/dependencies"
    internal val dependenciesJars = File(dependenciesJarsDir).listFiles()?.joinToString(classPathSeparator)
        ?: System.getProperty("java.class.path")
}

@Suppress("unused")
enum class Tool {
    UtBot {
        @Suppress("EXPERIMENTAL_API_USAGE")
        override fun run(
            project: ProjectToEstimate,
            cut: ClassUnderTest,
            timeLimit: Long,
            methodNameFilter: String?,
            globalStats: GlobalStats,
            compiledTestDir: File,
            classFqn: String
        ) {
            val classStats: StatsForClass = try {
                withUtContext(UtContext(project.classloader)) {
                    runGeneration(
                        cut,
                        timeLimit,
                        project.sootClasspathString,
                        runFromEstimator = true,
                        methodNameFilter
                    )
                }
            } catch (e: CancellationException) {
                logger.info { "[$classFqn] finished with CancellationException" }
                return
            } catch (e: Throwable) {
                logger.info { "ISOLATION: $e" }
                logger.info { "continue without compilation" }
                return
            }

            globalStats.statsForClasses.add(classStats)

            try {
                val testClass = cut.generatedTestFile
                classStats.testClassFile = testClass

                val cmd = arrayOf(
                    javacCmd,
                    "-d", compiledTestDir.absolutePath,
                    "-cp", project.compileClasspathString,
                    "-nowarn",
                    "-XDignore.symbol.file",
                    testClass.absolutePath
                )

                logger.info().bracket("Compiling class ${testClass.absolutePath}") {

                    logger.trace { cmd.toText() }
                    val process = Runtime.getRuntime().exec(cmd)

                    thread {
                        val errors = process.errorStream.reader().buffered().readText()
                        if (errors.isNotEmpty())
                            logger.error { "Compilation errors: $errors" }
                    }.join()


                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        logger.error { "Failed to compile test class ${cut.testClassSimpleName}" }
                        classStats.failedToCompile = true

                    } else {
                        val compilableTestSourcePath = Paths.get(
                            testClass.toString()
                                .replace("test_candidates", "test")
                        )
                        Files.createDirectories(compilableTestSourcePath.parent)
                        Files.move(
                            testClass.toPath(),
                            compilableTestSourcePath,
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        logger.info("Moved successfully compiled file into $compilableTestSourcePath")
                    }
                }
            } catch (t: Throwable) {
                logger.error(t) { "Unexpected exception while running external compilation process" }
            }
        }

        override fun moveProducedFilesIfNeeded() {
            // don't do anything
        }
    },
    EvoSuite {
        override fun run(
            project: ProjectToEstimate,
            cut: ClassUnderTest,
            timeLimit: Long,
            methodNameFilter: String?,
            globalStats: GlobalStats,
            compiledTestDir: File,
            classFqn: String
        ) {
            // EvoSuite has several phases, the variable below is responsible for assert generation
            // timeout. We want to give 10s for a big time budgets and timeLimit / 5 for small budgets.
            val timeLimitForGeneration = min(10, timeLimit / 5)

            val command = listOf(
                javaCmd, "-jar", evosuiteJarPath,
                "-generateSuite",
                "-Dsearch_budget=${timeLimit - timeLimitForGeneration}",
                "-Dstopping_condition=MaxTime",
                "-projectCP", "\"${project.compileClasspathString}\"",
                "-class", "\"$classFqn\"",
                "-D", "minimize=false",
                "-D", "minimize_second_pass=false",
                "-D", "minimization_timeout=0",
                "-D", "extra_timeout=0",
                "-D", "assertion_timeout=$timeLimitForGeneration",
                "-D", "junit_check=FALSE",
                "-D", "minimize_skip_coincidental=false",

                // because of some unknown reasons, the option below doesn't work
                // and the generated files will be stored in the root directory
                // in folders "evosuite-report" and "evosuite-tests"
                "-D", "OUTPUT_DIR=\"evosuite-tests-custom-folder\""
            )

            try {
                logger.info { "Started processing $classFqn" }
                val process = ProcessBuilder(command).redirectErrorStream(true).start()

                logger.info { "Pid: ${process.pid}" }

                process.inputStream.bufferedReader().use { reader ->
                    while (true) {
                        reader.readLine()?.let { logger.info { it } } ?: break
                    }
                }

                // EvoSuite in its way work with timeouts: it might generate 93 second for 90s timeout
                // or start to minimize code with disabled minimization. Moreover, it takes time to
                // analyze classpath (for now it is about 15 seconds). So we have to wait for a bit more
                // than we want. From some experience I put here 50 seconds, but it might not be enough.
                val timeoutForProcessSeconds = timeLimit + 50 // seconds
                if (!process.waitFor(timeoutForProcessSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly().waitFor()
                    logger.info { "Cancelled by timeout" }
                }

                logger.info { "Processed $classFqn, exit code: ${process.exitValue()}" }
            } catch (e: Throwable) {
                logger.error { "$classFqn: ${e.message}" }
            }
        }

        override fun moveProducedFilesIfNeeded() {
            val output = Paths.get(evosuiteOutputDir)
            Files.createDirectories(output)
            moveFolder(sourceFile = File("./evosuite-report"), targetFile = evosuiteReportFile)
            moveFolder(sourceFile = File("./evosuite-tests"), targetFile = evosuiteGeneratedTestsFile)
        }
    };

    abstract fun run(
        project: ProjectToEstimate,
        cut: ClassUnderTest,
        timeLimit: Long,
        methodNameFilter: String?,
        globalStats: GlobalStats,
        compiledTestDir: File,
        classFqn: String
    )

    abstract fun moveProducedFilesIfNeeded()
}

fun main(args: Array<String>) {
    val estimatorArgs: Array<String>
    val methodFilter: String?
    val projectFilter: List<String>?
    val processedClassesThreshold: Int
    val tools: List<Tool>

    // very special case when you run your project directly from IntellijIDEA omitting command line arguments
    if (args.isEmpty() && System.getProperty("os.name")?.run { contains("win", ignoreCase = true) } == true) {
        processedClassesThreshold = 9999 //change to change number of classes to run
        val timeLimit = 20 // increase if you want to debug something

        // Uncomment it for debug purposes:
        // you can specify method for test generation in format `classFqn.methodName`
        // examples:
//        methodFilter = "org.antlr.v4.codegen.model.decl.ContextGetterDecl.*"
//        methodFilter = "org.antlr.v4.parse.LeftRecursiveRuleWalker.*"
//        methodFilter = "com.google.common.graph.Graphs.*"
//        methodFilter = "com.google.common.math.LongMath.sqrt"
//        methodFilter = "org/antlr/v4/tool/ErrorManager.*"
//        methodFilter = null

//        projectFilter = listOf("samples", "utbottest")
//        projectFilter = listOf("samples")
//        tools = listOf(Tool.UtBot, Tool.EvoSuite)
//        tools = listOf(Tool.UtBot)
//        tools = listOf(Tool.EvoSuite)

        // config for SBST 2022
        methodFilter = null
        projectFilter = listOf("fastjson-1.2.50", "guava-26.0", "seata-core-0.5.0", "spoon-core-7.0.0")
        tools = listOf(Tool.UtBot)

        estimatorArgs = arrayOf(
            classesLists,
            jarsDir,
            "$timeLimit",
            outputDir,
            moduleTestDir
        )
    } else {
        require(args.size == 6) {
            "Wrong arguments: <classes dir> <classpath_dir> <time limit (s)> <output dir> <test dir> <junit jar path> expected, but got: ${args.toText()}"
        }
        logger.info { "Command line: [${args.joinToString(" ")}]" }

        estimatorArgs = args
        processedClassesThreshold = 9999
        methodFilter = null
        projectFilter = null
        tools = listOf(Tool.UtBot)
    }

    runEstimator(estimatorArgs, methodFilter, projectFilter, processedClassesThreshold, tools)
}


fun runEstimator(
    args: Array<String>,
    methodFilter: String?,
    projectFilter: List<String>?,
    processedClassesThreshold: Int,
    tools: List<Tool>
) {

    val classesLists = File(args[0])
    val classpathDir = File(args[1])
    val timeLimit = args[2].toLong()
    val outputDir = File(args[3])
    // we don't use it: val moduleTestDir = File(args[4])

    val testCandidatesDir = File(outputDir, "test_candidates")
    val compiledTestDir = File(outputDir, "compiled")
    compiledTestDir.mkdirs()
    val unzippedJars = File(outputDir, "unzipped")

//    Predictors.smt = UtBotTimePredictor()
//    Predictors.smtIncremental = UtBotTimePredictorIncremental()
//    Predictors.testName = StatementUniquenessPredictor()
    EngineAnalyticsContext.featureProcessorFactory = FeatureProcessorWithStatesRepetitionFactory()
    EngineAnalyticsContext.featureExtractorFactory = FeatureExtractorFactoryImpl()


    // fix for CTRL-ALT-SHIFT-C from IDEA, which copies in class#method form
    // fix for path form
    val updatedMethodFilter = methodFilter
        ?.replace('#', '.')
        ?.replace('/', '.')

    val classFqnFilter: String? = updatedMethodFilter?.substringBeforeLast('.')
    val methodNameFilter: String? = updatedMethodFilter?.substringAfterLast('.')?.let { if (it == "*") null else it }

    if (updatedMethodFilter != null)
        logger.info { "Filtering: class='$classFqnFilter', method ='$methodNameFilter'" }


    val projectToClassFQNs = classesLists.listFiles()!!.associate { it.name to File(it, "list").readLines() }

    val projects = mutableListOf<ProjectToEstimate>()

    logger.info { "Found ${projectToClassFQNs.size} projects" }
    for ((name, classesFQN) in projectToClassFQNs) {
        val project = ProjectToEstimate(
            name,
            classesFQN,
            File(classpathDir, name).listFiles()!!.filter { it.toString().endsWith("jar") },
            testCandidatesDir,
            unzippedJars
        )

        logger.info { "\n>>>" }
        logger.info { project }
        project.unzipConditionally()

        //smoke test
        project.classFQNs.forEach { fqn ->
            try {
                project.classloader.loadClass(fqn).kotlin
            } catch (e: Throwable) {
                logger.info { "Smoke test failed for class: $fqn" }
            }
        }

        projects.add(project)
    }

    val globalStats = GlobalStats()

    try {
        tools.forEach { tool ->
            var classIndex = 0

            outer@ for (project in projects) {
                if (projectFilter != null && project.name !in projectFilter) continue

                logger.info { "------------- project [${project.name}] ---- " }

                // take all the classes from the corresponding jar if a list of the specified classes is empty
                val extendedClassFqn = project.classFQNs.ifEmpty { project.classNames }

                for (classFqn in extendedClassFqn.filter { classFqnFilter?.equals(it) ?: true }) {
                    classIndex++
                    if (classIndex > processedClassesThreshold) {
                        logger.info { "Reached limit of $processedClassesThreshold classes" }
                        break@outer
                    }

                    val cut =
                        ClassUnderTest(
                            project.classloader.loadClass(classFqn).id,
                            project.outputTestSrcFolder,
                            project.unzippedDir
                        )

                    logger.info { "------------- [${project.name}] ---->--- [$classIndex:$classFqn] ---------------------" }

                    tool.run(project, cut, timeLimit, methodNameFilter, globalStats, compiledTestDir, classFqn)
                }
            }
        }
    } finally {
//        unzipDirectory.deleteRecursively()
//        Predictors.smt.terminate()
    }

    logger.info { globalStats }
    ConcreteExecutor.defaultPool.close()

    if (globalStats.statsForClasses.isNotEmpty())
        exitProcess(1)
}

private fun moveFolder(sourceFile: File, targetFile: File) {
    if (!targetFile.exists()) Files.createDirectories(targetFile.toPath())

    require(sourceFile.exists())
    require(targetFile.isDirectory)

    sourceFile.walk()
        .onLeave { if (it.isDirectory) Files.delete(it.toPath()) }
        .forEach { file ->
            val targetPath = Paths.get("${targetFile.path}${File.separator}${file.relativeTo(sourceFile)}")

            if (file.isDirectory) {
                Files.createDirectories(targetPath)
                return@forEach
            }

            if (file.isFile) {
                targetPath.parent?.let { Files.createDirectories(it) }

                Files.move(
                    file.toPath(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
}

private fun classNamesByJar(jar: File): List<String> {
    val zip = ZipInputStream(FileInputStream(jar))

    return generateSequence { zip.nextEntry }
        .filter { !it.isDirectory && it.name.endsWith(".class") }
        .map {
            val className = it.name.replace('/', '.')
            className.substringBeforeLast(".class")
        }
        .toList()
}

class ProjectToEstimate(
    val name: String,
    val classFQNs: List<String>,
    private val jars: List<File>,
    testCandidatesDir: File,
    unzippedJars: File
) {
    val outputTestSrcFolder = File(testCandidatesDir, name).apply { mkdirs() }
    val unzippedDir = File(unzippedJars, name)
    val classloader = URLClassLoader(jars.map { it.toUrl() }.toTypedArray(), null)
    val sootClasspathString get() = jars.joinToString(classPathSeparator)
    val compileClasspathString
        get() = arrayOf(outputTestSrcFolder.absolutePath, sootClasspathString, dependenciesJars)
            .joinToString(classPathSeparator)
    val classNames: List<String> = jars.flatMap { classNamesByJar(it) } // classes for the whole project

    fun unzipConditionally() {
        val lastModifiedMarker = File(unzippedDir, "lastModified")
        if (lastModifiedMarker.exists() && jars.all { it.lastModified() <= lastModifiedMarker.lastModified() }) {
            logger.info { "No need to unzip into `$unzippedDir`, it's already up-to-date" }
        } else {
            if (unzippedDir.exists()) {
                logger.info { "Deleting $unzippedDir" }
                unzippedDir.delete()
            }

            jars.forEach { jarFile ->
                logger.info { "Unzipping ${jarFile.name} ----> $unzippedDir" }
                FileUtil.extractArchive(jarFile.toPath(), unzippedDir.toPath())

                logger.info { "Touching ${lastModifiedMarker.absolutePath}" }
                lastModifiedMarker.writeText("touched")
            }
        }
    }

    override fun toString(): String = "Project '$name': [" +
            "\tclassFQNs: ${classFQNs.joinToString(", ")}\n" +
            "\tjars: ${jars.joinToString(classPathSeparator)}\n" +
            "\tgenerated test src folder: ${outputTestSrcFolder.absolutePath}\n"
}
