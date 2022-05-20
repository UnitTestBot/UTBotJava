package org.utbot.cli.writers

import java.io.PrintWriter

class FileWriter(filepath: String) : IWriter {
    private val writer: PrintWriter = PrintWriter(filepath)

    override fun append(info: String) {
        writer.append(info)
    }

    fun close() {
        writer.close()
    }
}