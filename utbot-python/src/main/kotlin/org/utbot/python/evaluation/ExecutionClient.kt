package org.utbot.python.evaluation

import org.utbot.python.utils.startProcess
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class ExecutionClient(
    private val hostname: String,
    private val port: Int,
    val pythonPath: String
) {
    private val clientSocket: Socket = Socket(hostname, port)
    private val outStream = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
    private val inStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
    private val process = runServer()

    private fun runServer() =
        startProcess(
            listOf(
                pythonPath,
                "-m",
                "utbot_executor",
                hostname,
                port.toString()
            )
        )

    fun stopServer() {
        stopConnection()
        process.destroy() // ?
    }

    fun sendMessage(msg: String) {
        outStream.write(msg)
        outStream.flush()
        outStream.write("END")
        outStream.flush()
    }

    fun receiveMessage(): String? {
        return inStream.readLine()
    }

    fun stopConnection() {
        inStream.close()
        outStream.close()
        clientSocket.close()
    }
}

fun main() {
    val client = ExecutionClient("localhost", 12011, "python")
    client.sendMessage("Firstfjlskdjf")
    client.sendMessage("Second")
    client.sendMessage("{123: 123}")
    client.sendMessage("STOP")
    client.stopConnection()
}