package org.utbot.rd

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val serverScheduler = UtSingleThreadScheduler { logger.info(it) }

inline fun <T> LifetimeDefinition.terminateOnException(block: (Lifetime) -> T): T {
    try {
        return block(this)
    } catch (e: Throwable) {
        this.terminate()
        throw e
    }
}

suspend fun <T> IScheduler.pump(lifetime: Lifetime, block: () -> T): T {
    val ldef = lifetime.createNested()
    val deferred = CompletableDeferred<T>()

    ldef.onTermination { deferred.cancel() }
    deferred.invokeOnCompletion { ldef.terminate() }

    this.invokeOrQueue {
        deferred.complete(block())
    }

    return deferred.await()
}

suspend fun <T> ISource<T>.adviseForConditionAsync(lifetime: Lifetime, condition: (T) -> Boolean): Deferred<Unit> {
    val ldef = lifetime.createNested()
    val deferred = CompletableDeferred<Unit>()

    ldef.onTermination { deferred.cancel() }
    deferred.invokeOnCompletion { ldef.terminate() }

    this.advise(ldef) {
        if(condition(it)) {
            deferred.complete(Unit)
        }
    }

    return deferred
}

suspend fun ISource<Boolean>.adviseForConditionAsync(lifetime: Lifetime): Deferred<Unit> {
    return this.adviseForConditionAsync(lifetime) {it}
}

/**
 * Process will not be started if lifetime is not alive
 */
suspend fun startUtProcessWithRdServer(
    lifetime: Lifetime? = null,
    factory: (Int) -> Process
): ProcessWithRdServer {
    lifetime?.throwIfNotAlive()

    val port = NetUtils.findFreePort(0)

    return factory(port).withRdServer(lifetime) {
        Protocol(
            "Server",
            Serializers(),
            Identities(IdKind.Server),
            serverScheduler,
            SocketWire.Server(it, serverScheduler, port, "ServerSocket"),
            it
        )
    }
}