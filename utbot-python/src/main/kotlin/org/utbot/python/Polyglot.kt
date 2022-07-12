package org.utbot.python

import org.graalvm.polyglot.Context

object Polyglot {
    @JvmStatic
    fun main(args: Array<String>) {
        val context = Context.newBuilder().allowIO(true).build()
        val array = context.eval("python", "[1,2,42,4]")
        val result = array.getArrayElement(2).asInt()
        println(result)
    }
}
