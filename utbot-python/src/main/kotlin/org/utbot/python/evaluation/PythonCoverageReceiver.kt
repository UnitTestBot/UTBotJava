package org.utbot.python.evaluation

import mu.KotlinLogging
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.math.max

class PythonCoverageReceiver(
    val until: Long,
) : Thread() {
    val coverageStorage = mutableMapOf<String, MutableSet<Int>>()
    private val socket = DatagramSocket()
    private val logger = KotlinLogging.logger {}

    init {
        updateSoTimeout()
    }

    private fun updateSoTimeout() {
        socket.soTimeout = max((until - System.currentTimeMillis()).toInt(), 0)
    }

    fun address(): Pair<String, String> {
        return "localhost" to socket.localPort.toString()
    }

    fun kill() {
        socket.close()
        this.interrupt()
    }

    override fun run() {
        try {
            while (true) {
                updateSoTimeout()
                val buf = ByteArray(256)
                val request = DatagramPacket(buf, buf.size)
                socket.receive(request)
                val (id, line) = request.data.decodeToString().take(request.length).split(":")
                val lineNumber = line.toInt()
                coverageStorage.getOrPut(id) { mutableSetOf() }.add(lineNumber)
            }
        } catch (ex: SocketException) {
            logger.debug { ex.message }
        } catch (ex: IOException) {
            logger.debug { "IO error: " + ex.message }
        } catch (ex: SocketTimeoutException) {
            logger.debug { "IO error: " + ex.message }
        }
    }
}