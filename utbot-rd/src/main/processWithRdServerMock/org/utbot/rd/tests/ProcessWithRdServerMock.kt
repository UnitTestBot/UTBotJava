package org.utbot.rd.tests

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.IScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): Unit {
    val def = LifetimeDefinition()
    val length = args.first().toLong()
    val shouldstart = args.last().toBoolean()
    val port = args[1].toInt()
    val scheduler = object : IScheduler {
        override val isActive = true

        override fun flush() {}

        override fun queue(action: () -> Unit) {
            action()
        }
    }

    if (shouldstart) {
        val protocol = Protocol(
            "TestClient",
            Serializers(),
            Identities(IdKind.Client),
            scheduler,
            SocketWire.Client(def, scheduler, port),
            def
        )
        println(protocol.name)
    }

    runBlocking {
        delay(length * 1000)
    }
    def.terminate()
}