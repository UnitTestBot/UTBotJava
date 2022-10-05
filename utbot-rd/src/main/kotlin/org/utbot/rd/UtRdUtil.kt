package org.utbot.rd

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.framework.util.synchronizeWith
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

// useful when initializing something
inline fun <T> LifetimeDefinition.terminateOnException(block: (Lifetime) -> T): T {
    try {
        return block(this)
    } catch (e: Throwable) {
        this.terminate()
        throw e
    }
}

// suspends until provided lifetime is terminated or coroutine cancelled
suspend fun Lifetime.awaitTermination() {
    val deferred = CompletableDeferred<Unit>()
    this.onTermination { deferred.complete(Unit) }
    deferred.await()
}

// function will return when block completed
// if coroutine was cancelled - CancellationException will be thrown
// if lifetime was terminated before block completed - CancellationException will be thrown
// lambda receives lifetime that indicates whether it's operation is still required
suspend fun <T> IScheduler.pump(lifetime: Lifetime, block: (Lifetime) -> T): T {
    val ldef = lifetime.createNested()
    val deferred = CompletableDeferred<T>()

    ldef.synchronizeWith(deferred)

    this.invokeOrQueue {
        deferred.complete(block(ldef))
    }

    return deferred.await()
}

// deferred will be completed if condition was met
// if condition no more needed - cancel deferred
// if lifetime was terminated before condition was met - deferred will be canceled
// if you need timeout - wrap returned deferred it in withTimeout
suspend fun <T> ISource<T>.adviseForConditionAsync(lifetime: Lifetime, condition: (T) -> Boolean): Deferred<Unit> {
    val ldef = lifetime.createNested()
    val deferred = CompletableDeferred<Unit>()

    ldef.synchronizeWith(deferred)

    this.advise(ldef) {
        if(condition(it)) {
            deferred.complete(Unit)
        }
    }

    return deferred
}

suspend fun ISource<Boolean>.adviseForConditionAsync(lifetime: Lifetime): Deferred<Unit> {
    return this.adviseForConditionAsync(lifetime) { it }
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
        val name = "Server$port"
        val rdServerProtocolScheduler = SingleThreadScheduler(it, "Scheduler for $name")
        Protocol(
            "Server",
            Serializers(),
            Identities(IdKind.Server),
            rdServerProtocolScheduler,
            SocketWire.Server(it, rdServerProtocolScheduler, port, "ServerSocket"),
            it
        )
    }
}