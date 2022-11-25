package org.utbot.rd.exceptions

/**
 * This exception is designed to be thrown any time you start child process with rd,
 * but it dies before rd initiated, implicating that the problem probably in CLI arguments
 */
abstract class InstantProcessDeathException(private val debugPort: Int, private val isProcessDebug: Boolean) : Exception() {
    override val message: String?
        get() {
            var text = "Process died before any request was executed, check process log file."
            if (isProcessDebug) {
                text += " Process requested debug on port - $debugPort, check if port is open."
            }
            return text
        }
}