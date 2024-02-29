package org.utbot.python.engine

import mu.KotlinLogging
import org.usvm.runner.StandardLayout
import org.usvm.runner.USVMPythonConfig
import org.usvm.runner.USVMPythonFunctionConfig
import org.usvm.runner.USVMPythonMethodConfig
import org.usvm.runner.USVMPythonRunConfig
import org.usvm.runner.venv.extractVenvConfig
import org.utbot.python.MypyConfig
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.engine.fuzzing.FuzzingEngine
import org.utbot.python.engine.symbolic.SymbolicEngine
import org.utbot.python.engine.symbolic.SymbolicExecutionEvaluator
import org.utbot.python.engine.symbolic.USVMPythonAnalysisResultReceiverImpl
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.constants.ConstantCollector
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.mypy.GlobalNamesStorage
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.pythonName
import org.utbot.python.utils.convertToTime
import java.io.File
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

class GlobalPythonEngine(
    val method: PythonMethod,
    val configuration: PythonTestGenerationConfig,
    private val mypyConfig: MypyConfig,
    val until: Long,
) {
    val executionStorage = ExecutionStorage()
    private val typeStorage = PythonTypeHintsStorage.get(mypyConfig.mypyStorage)
    private val constantCollector = ConstantCollector(typeStorage)
    private val hintCollector = constructHintCollector(
        mypyConfig.mypyStorage,
        typeStorage,
        constantCollector,
        method,
        configuration.testFileInformation.moduleName
    )

    private fun runFuzzing() {
        FuzzingEngine(
            method,
            configuration,
            typeStorage,
            hintCollector,
            constantCollector,
            mypyConfig.mypyStorage,
            mypyConfig.mypyReportLine,
            until,
            executionStorage,
        ).start()
    }

    private fun runSymbolic(debug: Boolean = false) {
        logger.info { "Symbolic until: ${until.convertToTime()}" }
        val usvmPythonConfig = USVMPythonConfig(
            StandardLayout(File(configuration.usvmConfig.usvmDirectory)),
            configuration.usvmConfig.javaCmd,
            mypyConfig.mypyBuildDirectory.root.canonicalPath,
            configuration.sysPathDirectories,
            extractVenvConfig(configuration.pythonPath),
            configuration.pathSelector
        )
        val runner = SymbolicExecutionEvaluator(
            method,
            configuration,
            executionStorage,
            until
        )
        runner.start()
        val receiver = USVMPythonAnalysisResultReceiverImpl(runner.executionQueue)
        val config = if (method.containingPythonClass == null) {
            USVMPythonFunctionConfig(configuration.testFileInformation.moduleName, method.name)
        } else {
            USVMPythonMethodConfig(
                configuration.testFileInformation.moduleName,
                method.name,
                method.containingPythonClass!!.pythonName()
            )
        }
        val engine = SymbolicEngine(
            usvmPythonConfig,
            configuration,
        )
        val usvmConfig = USVMPythonRunConfig(config, until - System.currentTimeMillis(), configuration.timeoutForRun * 2)
        if (debug) {
            engine.debugRun(usvmConfig)
        } else {
            engine.analyze(
                usvmConfig,
                receiver
            )
        }
        logger.info { "Symbolic: stopped receiver" }
        runner.receiverFinished = true
        runner.join()
        runner.close()
        logger.info { "Symbolic: stopped runner" }
    }

    fun run() {
        val threads = mutableListOf<Thread>()
        if (configuration.inputSearchMode.fuzzingEnabled) {
            val fuzzing = thread(
                start = true,
                isDaemon = false,
                name = "Fuzzer"
            ) {
                logger.info { " >>>>>>> Start fuzzer >>>>>>> " }
                runFuzzing()
                logger.info { " <<<<<<< Finish fuzzer <<<<<<< " }
            }
            threads.add(fuzzing)
        } else {
            logger.info { "Fuzzing disabled." }
        }
        if (configuration.inputSearchMode.symbolicEnabled) {
            val symbolic = thread(
                start = true,
                isDaemon = false,
                name = "Symbolic"
            ) {
                logger.info { " ------- Start symbolic ------- " }
                runSymbolic()
                logger.info { " ======= Finish symbolic ======= " }
            }
            threads.add(symbolic)
        } else {
            logger.info { "Symbolic disabled." }
        }
        threads.forEach { it.join() }
    }

    fun debugUsvmRun() {
        val symbolic = thread(
            start = true,
            isDaemon = true,
            name = "Symbolic"
        ) {
            logger.info { " ...... Checking symbolic ...... " }
            runSymbolic(debug = true)
        }
        symbolic.join()
    }

    private fun constructHintCollector(
        mypyStorage: MypyInfoBuild,
        typeStorage: PythonTypeHintsStorage,
        constantCollector: ConstantCollector,
        method: PythonMethod,
        moduleName: String,
    ): HintCollector {

        // initialize definitions first
        mypyStorage.definitions[moduleName]!!.values.map { def ->
            def.getUtBotDefinition()
        }

        val mypyExpressionTypes = mypyStorage.exprTypes[moduleName]?.let { moduleTypes ->
            moduleTypes.associate {
                Pair(it.startOffset.toInt(), it.endOffset.toInt() + 1) to it.type.asUtBotType
            }
        } ?: emptyMap()

        val namesStorage = GlobalNamesStorage(mypyStorage)
        val hintCollector = HintCollector(method, typeStorage, mypyExpressionTypes, namesStorage, moduleName)
        val visitor = Visitor(listOf(hintCollector, constantCollector))
        visitor.visit(method.ast)
        return hintCollector
    }
}
