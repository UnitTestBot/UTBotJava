package org.utbot.python.evaluation

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class PythonWorker(
    private val clientSocket: Socket
) {
    private val outStream = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream(), "UTF8"))
    private val inStream = BufferedReader(InputStreamReader(clientSocket.getInputStream(), "UTF8"))

    fun stop() {
        outStream.write("STOP")
        outStream.flush()
    }

    fun sendData(msg: String) {
        outStream.write("DATA")

        val size = msg.encodeToByteArray().size
        outStream.write(size.toString().padStart(16))
        outStream.flush()

        outStream.write(msg)
        outStream.flush()
    }

    fun receiveMessage(): String? {
        val lengthLine = inStream.readLine()
        if (lengthLine == null) {
            return null
        }
        val length = lengthLine.toInt()
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