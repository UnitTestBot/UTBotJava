package org.utbot.python.evaluation

import mu.KotlinLogging
import java.net.SocketException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private val logger = KotlinLogging.logger {}

class UtExecutorThread {
    enum class Status {
        TIMEOUT,
        OK,
    }

    companion object {
        fun run(worker: PythonWorker, executionTimeout: Long): Pair<Status, String?> {
            val executor = Executors.newSingleThreadExecutor()
            val future = executor.submit(Task(worker))

            logger.info("Running with timeout $executionTimeout")

            val result = try {
                Status.OK to future.get(executionTimeout, TimeUnit.MILLISECONDS)
            } catch (ex: TimeoutException) {
                future.cancel(true)
                Status.TIMEOUT to null
            }

            logger.info("Stopped running. Result: ${result.first}")

            executor.shutdown()
            return result
        }
    }
}

class Task(
    private val worker: PythonWorker
) : Callable<String?> {
    override fun call(): String? {
        return try {
            worker.receiveMessage()
        } catch (ex: SocketException) {
            null
        }
    }
}