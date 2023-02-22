package org.utbot.python.evaluation

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class PythonWorker(
    private val clientSocket: Socket
) {
    private val outStream = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
    private val inStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

    fun stop() {
        outStream.write("STOP")
        outStream.flush()
    }

    fun sendData(msg: String) {
        outStream.write("DATA")

        val size = msg.length
        outStream.write(size.toString().padStart(16))

        outStream.write(msg)
        outStream.flush()
    }

    fun receiveMessage(): String {
        val length = inStream.readLine().toInt()
        val buffer = CharArray(length)
        inStream.read(buffer)
        return String(buffer)
    }

    private fun stopConnection() {
        inStream.close()
        outStream.close()
        clientSocket.close()
    }

    fun stopServer() {
        stop()
        stopConnection()
    }

}