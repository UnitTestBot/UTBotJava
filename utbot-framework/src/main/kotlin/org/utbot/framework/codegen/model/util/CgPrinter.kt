package org.utbot.framework.codegen.model.util

interface CgPrinter {
    fun print(text: String)
    fun println(text: String = "")

    fun pushIndent()
    fun popIndent()

    override fun toString(): String
}

class CgPrinterImpl(
    // TODO: maybe it should be instantiated with some initial capacity
    private var tabsAmount: Int = 0,
    private val builder: StringBuilder = StringBuilder()
) : CgPrinter, Appendable by builder {

    private var atLineStart: Boolean = true

    private val indent: String get() = TAB * tabsAmount

    override fun pushIndent() {
        tabsAmount++
    }

    override fun popIndent() {
        tabsAmount--
    }

    override fun toString(): String = builder.toString()

    override fun print(text: String) {
        if (atLineStart) {
            printIndent()
            atLineStart = false
        }
        append(text)
    }

    override fun println(text: String) {
        if (atLineStart) {
            printIndent()
            atLineStart = false
        }
        print(text)
        appendLine()
        atLineStart = true
    }

    fun printLine() {
        appendLine()
    }

    private fun printIndent() {
        append(indent)
    }

    private operator fun String.times(n: Int): String = repeat(n)

    companion object {
        private const val TAB = "    "
    }
}