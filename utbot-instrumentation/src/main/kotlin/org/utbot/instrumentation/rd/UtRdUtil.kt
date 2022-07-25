package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import mu.KotlinLogging

private val logger = KotlinLogging.logger("UtRd")
private val serverScheduler = UtSingleThreadScheduler { logger.info(it) }

object UtRdUtil {
    fun createUtServerProtocol(
        lifetime: Lifetime,
        port: Int = 0,
        scheduler: IScheduler = serverScheduler,
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

    fun createUtClientProtocol(
        lifetime: Lifetime,
        port: Int, // client can only talk to provided port
        scheduler: IScheduler, // do not use server scheduler
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

    suspend fun startUtProcessWithRdServer(
        parent: Lifetime? = null,
        factory: (Int) -> Process
    ): ProcessWithRdServer {
        val port = NetUtils.findFreePort(0)

        return factory(port).withRdServer(parent) {
            createUtServerProtocol(it, port)
        }
    }
}