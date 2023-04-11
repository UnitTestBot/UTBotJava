package org.utbot.python.evaluation

import java.net.SocketException

class UtExecutorThread : Thread() {
    override fun run() {
        response = try {
            pythonWorker?.receiveMessage()
        } catch (ex: SocketException) {
            null
        }
    }

    enum class Status {
        TIMEOUT,
        OK,
    }

    companion object {
        var pythonWorker: PythonWorker? = null
        var response: String? = null

        fun run(worker: PythonWorker, executionTimeout: Long): Pair<Status, String?> {
            pythonWorker = worker
            response = null
            val thread = UtExecutorThread()
            thread.start()
            // Wait for the thread to finish
            val finishTime = System.currentTimeMillis() + executionTimeout
            while (thread.isAlive && System.currentTimeMillis() < finishTime) {
                sleep(1)
            }
            val status = if (thread.isAlive) {
                thread.interrupt()
                Status.TIMEOUT
            } else {
                Status.OK
            }
            return status to response
        }
    }
}
