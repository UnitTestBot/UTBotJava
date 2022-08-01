package org.utbot.rd.tests

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val length = args.single().toLong()

    runBlocking {
        delay(length * 1000)
    }
}