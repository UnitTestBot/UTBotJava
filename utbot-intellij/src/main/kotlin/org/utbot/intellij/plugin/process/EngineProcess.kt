package org.utbot.intellij.plugin.process

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.DefaultExternalSystemJdkProvider
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.utbot.common.AbstractSettings
import org.utbot.common.getPid
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.CodeGeneratorResult
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.plugin.services.WorkingDirService
import org.utbot.framework.process.generated.*
import org.utbot.framework.util.ConflictTriggers
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.rdPortArgument
import org.utbot.rd.startUtProcessWithRdServer
import soot.SootMethod
import soot.util.HashChain
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.pathString
import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

private val logger = KotlinLogging.logger{}
private val engineProcessLogDirectory = utBotTempDirectory.toFile().resolve("rdEngineProcessLogs")

class EngineProcess(val lifetime:Lifetime) {
    private val id = Random.nextLong()
    private var count = 0

    companion object {
        private var configPath: Path? = null
        private fun getOrCreateLogConfig(): String {
            var realPath = configPath
            if (realPath == null) {
                synchronized(this) {
                    realPath = configPath
                    if (realPath == null) {
                        utBotTempDirectory.toFile().mkdirs()
                        configPath = Files.writeString(utBotTempDirectory.toFile().resolve("EngineProcess_log4j2.xml").toPath(), """<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <ThresholdFilter level="DEBUG"  onMatch="NEUTRAL"   onMismatch="DENY"/>
            <PatternLayout pattern="%d{HH:mm:ss.SSS} | %-5level | %c{1} | %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="debug">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>""")
                        realPath = configPath
                    }
                }
            }
            return realPath!!.pathString
        }
    }
    // because we cannot load idea bundled lifetime or it will break everything

    private fun debugArgument(): String {
        return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=5005".takeIf{ Settings.runIdeaProcessWithDebug} ?: ""
    }

    private val kryoHelper = KryoHelper(lifetime).apply {
        addInstantiator(HashChain::class.java) {
            HashChain<Any>()
        }
        addInstantiator(soot.UnitPatchingChain::class.java) {
            soot.UnitPatchingChain(HashChain())
        }

        addInstantiator(Collections.synchronizedCollection(mutableListOf<SootMethod>()).javaClass) {
            Collections.synchronizedCollection(mutableListOf<SootMethod>())
        }
        addInstantiator(Collections.synchronizedCollection(mutableListOf<Any>()).javaClass) {
            Collections.synchronizedCollection(mutableListOf<Any>())
        }

        Collections::class.java.declaredClasses
    }

    private suspend fun engineModel(): EngineProcessModel {
        lifetime.throwIfNotAlive()
        return lock.withLock {
            var proc = current

            if (proc == null) {
                proc = startUtProcessWithRdServer(lifetime) { port ->
                    val current = JdkInfoDefaultProvider().info
                    val required = JdkInfoService.jdkInfoProvider.info
                    val java =
                        JdkInfoService.jdkInfoProvider.info.path.resolve("bin${File.separatorChar}java").toString()
                    val cp = (this.javaClass.classLoader as PluginClassLoader).classPath.baseUrls.joinToString(
                        separator = ";",
                        prefix = "\"",
                        postfix = "\""
                    )
                    val classname = "org.utbot.framework.process.EngineMainKt"
                    val javaVersionSpecificArguments =
                        listOf("--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED", "--illegal-access=warn")
                            .takeIf { JdkInfoService.provide().version > 8 }
                            ?: emptyList()
                    val directory = WorkingDirService.provide().toFile()
                    val log4j2ConfigFile = "\"-Dlog4j2.configurationFile=${getOrCreateLogConfig()}\""
                    val debugArg = debugArgument()
                    logger.info { "java - $java\nclasspath - $cp\nport - $port" }
                    val cmd = mutableListOf<String>(java, "-ea")
                    if (javaVersionSpecificArguments.isNotEmpty()) {
                        cmd.addAll(javaVersionSpecificArguments)
                    }
                    if (debugArg.isNotEmpty()) {
                        cmd.add(debugArg)
                    }
                    cmd.add(log4j2ConfigFile)
                    cmd.add("-cp")
                    cmd.add(cp)
                    cmd.add(classname)
                    cmd.add(rdPortArgument(port))
                    ProcessBuilder(cmd).directory(directory).apply {
                        if (UtSettings.logConcreteExecutionErrors) {
                            engineProcessLogDirectory.mkdirs()
                            val logFile = File(engineProcessLogDirectory, "${id}-${count++}.log")
                            logger.info { "logFile - ${logFile.canonicalPath}" }
                            redirectOutput(logFile)
                            redirectError(logFile)
                        }
                    }.start().apply {
                        logger.debug { "Engine process started with PID = ${this.getPid}" }
                    }
                }.initModels {
                    engineProcessModel
                    settingsModel.settingFor.set { params ->
                        SettingForResult(AbstractSettings.allSettings[params.key]?.let {settings: AbstractSettings ->
                            val members: Collection<KProperty1<AbstractSettings, *>> = settings.javaClass.kotlin.memberProperties
                            val names: List<KProperty1<AbstractSettings, *>> = members.filter { it.name == params.propertyName }
                            val sing: KProperty1<AbstractSettings, *> = names.single()
                            val result = sing.get(settings)
                            logger.trace {"request for settings ${params.key}:${params.propertyName} - $result"}
                            result.toString()
                        })
                    }
                }.awaitSignal()
                current = proc
            }

            proc.protocol.engineProcessModel
        }
    }

    private val lock = Mutex()
    private var current: ProcessWithRdServer? = null

    fun setupUtContext(classpathForUrlsClassloader: List<String>) = runBlocking {
        engineModel().setupUtContext.startSuspending(lifetime, SetupContextParams(classpathForUrlsClassloader))
    }

    // suppose that only 1 simultaneous test generator process can be executed in idea
    // so every time test generator is created - we just overwrite previous
    fun createTestGenerator(buildDir: String, classPath: String?, dependencyPaths: String, jdkInfo: JdkInfo,isCancelled: (Unit) -> Boolean) = runBlocking {
        engineModel().isCancelled.set(handler = isCancelled)
        engineModel().createTestGenerator.startSuspending(lifetime, TestGeneratorParams(buildDir, classPath, dependencyPaths, JdkInfo(jdkInfo.path.pathString, jdkInfo.version)))
    }

    fun generate(
        mockInstalled: Boolean,
        staticsMockingIsConfigured: Boolean,
        conflictTriggers: ConflictTriggers,
        methods: List<ExecutableId>,
        mockStrategyApi: MockStrategyApi,
        chosenClassesToMockAlways: Set<ClassId>,
        timeout: Long,
        generationTimeout: Long,
        isSymbolicEngineEnabled: Boolean,
        isFuzzingEnabled: Boolean,
        fuzzingValue: Double,
        searchDirectory: String
        ): List<UtMethodTestSet> = runBlocking {
        val result = engineModel().generate.startSuspending(
            lifetime,
            GenerateParams(
                mockInstalled,
                staticsMockingIsConfigured,
                kryoHelper.writeObject(conflictTriggers.toMutableMap()),
                kryoHelper.writeObject(methods),
                mockStrategyApi.name,
                kryoHelper.writeObject(chosenClassesToMockAlways),
                timeout,
                generationTimeout,
                isSymbolicEngineEnabled,
                isFuzzingEnabled,
                fuzzingValue,
                searchDirectory
            )
        )

        return@runBlocking kryoHelper.readObject<List<UtMethodTestSet>>(result.notEmptyCases)
    }

    fun render(
        classUnderTest: ClassId,
        paramNames: MutableMap<ExecutableId, List<String>>,
        generateUtilClassFile: Boolean,
        testFramework: TestFramework,
        mockFramework: MockFramework,
        staticsMocking: StaticsMocking,
        forceStaticMocking: ForceStaticMocking,
        generateWarningsForStaticsMocking: Boolean,
        codegenLanguage: CodegenLanguage,
        parameterizedTestSource: ParametrizedTestSource,
        runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour,
        hangingTestSource: HangingTestsTimeout,
        enableTestsTimeout: Boolean,
        testClassPackageName: String,
        testSets: List<UtMethodTestSet>
    ): CodeGeneratorResult = runBlocking {
        return@runBlocking kryoHelper.readObject(
            engineModel().render.startSuspending(
                lifetime, RenderParams(
                    kryoHelper.writeObject(classUnderTest),
                    kryoHelper.writeObject(paramNames),
                    generateUtilClassFile,
                    testFramework.id.lowercase(),
                    mockFramework.name,
                    codegenLanguage.name,
                    parameterizedTestSource.name,
                    staticsMocking.id,
                    kryoHelper.writeObject(forceStaticMocking),
                    generateWarningsForStaticsMocking,
                    runtimeExceptionTestsBehaviour.name,
                    hangingTestSource.timeoutMs,
                    enableTestsTimeout,
                    testClassPackageName,
                    kryoHelper.writeObject(testSets)
                )
            ).codeGenerationResult
        )
    }

    fun forceTermination() = runBlocking {
        engineModel().stopProcess.start(Unit)
        current?.terminate()
    }

    init {
        lifetime.onTermination {
            current?.terminate()
        }
    }
}