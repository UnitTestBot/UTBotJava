package org.utbot.python.engine

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.engine.fuzzing.FuzzingEngine
import org.utbot.python.engine.symbolic.DummySymbolicEngine
import org.utbot.python.engine.symbolic.USVMPythonAnalysisResultReceiver
import org.utbot.python.engine.symbolic.USVMPythonFunctionConfig
import org.utbot.python.engine.symbolic.USVMPythonRunConfig
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.constants.ConstantCollector
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.mypy.GlobalNamesStorage
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.mypy.MypyReportLine
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

class GlobalPythonEngine(
    val method: PythonMethod,
    val configuration: PythonTestGenerationConfig,
    val mypyStorage: MypyInfoBuild,
    val mypyReport: List<MypyReportLine>,
    val until: Long,
) {
    val executionStorage = ExecutionStorage()
    val typeStorage = PythonTypeHintsStorage.get(mypyStorage)
    val constantCollector = ConstantCollector(typeStorage)
    val hintCollector = constructHintCollector(
        mypyStorage,
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
            mypyStorage,
            mypyReport,
            until,
            executionStorage,
        ).start()
    }

    private fun runSymbolic() {
        DummySymbolicEngine(
            configuration,
            executionStorage,
        ).analyze(
            USVMPythonRunConfig(
                USVMPythonFunctionConfig(method.moduleFilename, method.name),
                configuration.timeout,
                configuration.timeoutForRun
            ),
            USVMPythonAnalysisResultReceiver(method, configuration, System.currentTimeMillis() + configuration.timeout)
        )
    }

    fun run() {
        val fuzzing = thread(
            start = true,
            isDaemon = true,
            name = "Fuzzer"
        ) {
            logger.info { " ======= Start fuzzer ======= " }
            runFuzzing()
            logger.info { " ======= Finish fuzzer ======= " }
        }
        val symbolic = thread(
            start = true,
            isDaemon = true,
            name = "Symbolic"
        ) {
            logger.info { " ======= Start symbolic ======= " }
            runSymbolic()
            logger.info { " ======= Finish symbolic ======= " }
        }
        fuzzing.join()
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
        val hintCollector = HintCollector(method.definition, typeStorage, mypyExpressionTypes, namesStorage, moduleName)
        val visitor = Visitor(listOf(hintCollector, constantCollector))
        visitor.visit(method.ast)
        return hintCollector
    }
}