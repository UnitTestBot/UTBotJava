package org.utbot.python.engine.symbolic

import mu.KotlinLogging
import org.usvm.runner.USVMPythonAnalysisResultReceiver

private val logger = KotlinLogging.logger {}

class USVMPythonAnalysisResultReceiverImpl(
    private val threadSafeQueue: MutableList<String>
) : USVMPythonAnalysisResultReceiver() {

    override fun receivePickledInputValues(pickledTuple: String) {
        logger.debug { "SYMBOLIC: $pickledTuple" }
        threadSafeQueue.add(pickledTuple)
    }
}
