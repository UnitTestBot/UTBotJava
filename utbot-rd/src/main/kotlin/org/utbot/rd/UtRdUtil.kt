package org.utbot.rd

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.framework.util.synchronizeWith
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

suspend fun <T> ProcessWithRdServer.onScheduler(block: () -> T): T {
    val deffered = CompletableDeferred<T>()
    protocol.scheduler.invokeOrQueue { deffered.complete(block()) }
    return deffered.await()
}

fun <T> ProcessWithRdServer.onSchedulerBlocking(block: () -> T): T = runBlocking { onScheduler(block) }

fun <TReq, TRes> IRdCall<TReq, TRes>.startBlocking(req: TReq): TRes {
    val call = this
    // We do not use RdCall.sync because it requires timeouts for execution, after which request will be stopped.
    // Some requests, for example test generation, might be either really long, or have their own timeouts.
    // To honour their timeout logic we do not use RdCall.sync.
    return runBlocking { call.startSuspending(req) }
}

/**
 * Terminates lifetime if exception occurs.
 * Useful when initializing classes, for ex. if an exception occurs while some parts already bound to lifetime,
 * and you need to terminate those parts
 */
inline fun <T> LifetimeDefinition.terminateOnException(block: (Lifetime) -> T): T {
    try {
        return block(this)
    } catch (e: Throwable) {
        this.terminate()
        throw e
    }
}

/**
 * Suspend until provided lifetime terminates or coroutine cancelles
 */
suspend fun Lifetime.awaitTermination() {
    val deferred = CompletableDeferred<Unit>()
    this.onTermination { deferred.complete(Unit) }
    deferred.await()
}

/**
 * Executes block on IScheduler and suspends until completed
 * @param lifetime indicates whether it's operation is still required
 * @throws CancellationException if coroutine was cancelled or lifetime was terminated before block completed
*/
suspend fun <T> IScheduler.pump(lifetime: Lifetime, block: (Lifetime) -> T): T {
    val ldef = lifetime.createNested()
    val deferred = CompletableDeferred<T>()

    ldef.synchronizeWith(deferred)

    this.invokeOrQueue {
        deferred.complete(block(ldef))
    }

    return deferred.await()
}

suspend fun <T> IScheduler.pump(block: (Lifetime) -> T): T = this.pump(Lifetime.Eternal, block)

/**
 * Asynchronously checks the condition.
 * The condition can be satisfied or canceled.
 * As soon as the condition is checked, the lifetime is terminated.
 * In order to cancel the calculation, use the function return value of Deferred type.
 *
 * @see kotlinx.coroutines.withTimeout coroutine builder in case you need cancel the calculation by timeout.
 */
fun <T> ISource<T>.adviseForConditionAsync(lifetime: Lifetime, condition: (T) -> Boolean): Deferred<Unit> {
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

fun ISource<Boolean>.adviseForConditionAsync(lifetime: Lifetime): Deferred<Unit> {
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

inline fun <T> Logger.logMeasure(tag: String = "", block: () -> T): T {
    val start = System.currentTimeMillis()
    this.debug { "started evaluating $tag" }
    try {
        return block()
    }
    catch (e: Throwable) {
        this.debug { "exception during evaluating $tag: ${e.stackTraceToString()}" }
        throw e
    }
    finally {
        this.debug { "evaluating $tag took ${System.currentTimeMillis() - start} ms" }
    }
}