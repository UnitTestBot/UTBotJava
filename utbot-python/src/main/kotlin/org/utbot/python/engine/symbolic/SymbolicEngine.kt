package org.utbot.python.engine.symbolic

import mu.KotlinLogging
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.engine.ExecutionStorage

private val logger = KotlinLogging.logger {}

interface SymbolicEngineApi {
    fun analyze(runConfig: USVMPythonRunConfig, receiver: USVMPythonAnalysisResultReceiver)
}

class DummySymbolicEngine(
    val configuration: PythonTestGenerationConfig,
    val executionStorage: ExecutionStorage,
) : SymbolicEngineApi {
    override fun analyze(runConfig: USVMPythonRunConfig, receiver: USVMPythonAnalysisResultReceiver) {
        val endTime = System.currentTimeMillis() + runConfig.timeoutMs
        val symbolicCancellation = { configuration.isCanceled() || System.currentTimeMillis() > endTime }
        while (!symbolicCancellation()) {
            receiver.receivePickledInputValues("""b'\x80\x04\x95\x11\x00\x00\x00\x00\x00\x00\x00K\x02]\x94(K\x01K\x02e\x86\x94}\x94\x86\x94.'""")?.let {
                logger.info { "SYMBOLIC: save new execution" }
                executionStorage.saveSymbolicExecution(it)
            }
            Thread.sleep(100)
        }
    }

}

class SymbolicEngine(
    val usvmPythonConfig: USVMPythonConfig,
    val configuration: PythonTestGenerationConfig,
    val executionStorage: ExecutionStorage,
) : SymbolicEngineApi {
    override fun analyze(runConfig: USVMPythonRunConfig, receiver: USVMPythonAnalysisResultReceiver) {
        receiver.receivePickledInputValues("")?.let {
            executionStorage.saveSymbolicExecution(it)
        }
        TODO()
    }
}