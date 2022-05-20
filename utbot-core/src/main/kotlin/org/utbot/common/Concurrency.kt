package org.utbot.common

import java.util.concurrent.locks.Lock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

inline fun <reified T> Lock.withLockInterruptibly(action: () -> T): T {
    lockInterruptibly()
    try {
        return action()
    } finally {
        unlock()
    }
}

private fun cancelJobByPredicate(job: Job, isCanceled: () -> Boolean) {
    GlobalScope.launch {
        while (job.isActive) {
            if (isCanceled())
                job.cancel()
            yield() //spin
        }
    }
}

/**
 * Throws CancellationException if
 */
fun <T> runBlockingWithCancellationPredicate(isCanceled: () -> Boolean, block: suspend CoroutineScope.() -> T): T {
    if (isCanceled()) throw CancellationException()

    return runBlocking {
        val job = async { block() }
        cancelJobByPredicate(job, isCanceled)
        job.await()
    }
}

fun runIgnoringCancellationException(block: () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        //ignored
    }
}

fun currentThreadInfo() = Thread.currentThread().run { "$id: $name" }