package org.utbot.python.evaluation

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.CharBuffer

class ExecutionClient(
    hostname: String,
    port: Int
) {
    private val clientSocket: Socket = Socket(hostname, port)
    private val outStream = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
    private val inStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

    init {
        runServer()
    }

    private fun runServer() {
        TODO()
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
    val client = ExecutionClient("localhost", 12011)
    client.sendMessage("Firstfjlskdjf")
    client.sendMessage("Second")
    client.sendMessage("{123: 123}")
    client.sendMessage("STOP")
    client.stopConnection()
}