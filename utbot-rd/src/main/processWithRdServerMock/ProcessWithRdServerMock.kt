package org.utbot.rd.tests

import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.IScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.utbot.rd.UtRdUtil

fun main(args: Array<String>): Unit {
    val def = LifetimeDefinition()
    val length = args.first().toLong()
    val shouldstart = args.last().toBoolean()
    val port = args[1].toInt()

    if (shouldstart) {
        val protocol = UtRdUtil.createUtClientProtocol(def, port, object: IScheduler {
            override val isActive = true

            override fun flush() {}

            override fun queue(action: () -> Unit) {
                action()
            }
        })
    }

    runBlocking {
        delay(length * 1000)
    }
    def.terminate()
}