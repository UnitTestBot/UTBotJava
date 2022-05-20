package org.utbot.cli.writers

class ConsoleWriter : IWriter {
    override fun append(info: String) {
        println(info)
    }
}