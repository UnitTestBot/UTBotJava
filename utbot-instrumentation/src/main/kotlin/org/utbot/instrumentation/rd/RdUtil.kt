package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler

object RdUtil {
    fun createServer(
        lifetime: Lifetime,
        port: Int? = null,
        scheduler: IScheduler = DummyScheduler,
        name: String = "Server", // Protocol name aka location
        socketName: String = "ServerSocket", // Socket name
        serializers: ISerializers = Serializers()
    ): Protocol {
        return Protocol(
            name,
            serializers,
            Identities(IdKind.Server),
            scheduler,
            SocketWire.Server(lifetime, scheduler, port, socketName),
            lifetime
        )
    }


    fun createClient(
        lifetime: Lifetime,
        port: Int, // client can only talk to provided port
        scheduler: IScheduler = DummyScheduler,
        name: String = "Client", // Protocol name
        socketName: String = "ClientSocket", // Socket name
        serializers: ISerializers = Serializers()
    ): Protocol {
        return Protocol(
            name,
            serializers,
            Identities(IdKind.Client),
            scheduler,
            SocketWire.Client(lifetime, scheduler, port, socketName),
            lifetime
        )
    }
}

private object DummyScheduler : IScheduler {
    override fun flush() {}
    override fun queue(action: () -> Unit) = action()
    override val isActive: Boolean get() = true
}