package org.utbot.python.evaluation

import mu.KotlinLogging
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException

class PythonCoverageReceiver(
    private val port: Int,
) {
    val coverageStorage = mutableMapOf<String, MutableSet<Int>>()
    private val socket = DatagramSocket(port)
    private val logger = KotlinLogging.logger {}

    private fun run() {
        try {
            while (true) {
                val request = DatagramPacket(byteArrayOf(1), 1)
                socket.receive(request)
                println(request.data)
            }
        } catch (ex: SocketException) {
            logger.error("Socket error: " + ex.message);
        } catch (ex: IOException) {
            logger.error("IO error: " + ex.message);
        }
    }
}