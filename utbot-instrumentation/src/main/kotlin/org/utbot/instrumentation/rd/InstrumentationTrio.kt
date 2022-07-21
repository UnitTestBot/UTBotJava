package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.lifetime.Lifetime
import java.io.File
import java.util.concurrent.CountDownLatch

fun obtainClientIO(lifetime: Lifetime, protocol: Protocol, pid: Int): InstrumentationTrio {
    val latch = CountDownLatch(2)
    val mainToProcess = RdSignal<ByteArray>().static(1).init(lifetime, protocol, latch)
    val processToMain = RdSignal<ByteArray>().static(2).init(lifetime, protocol, latch)

    latch.await()
    signalChildReady(pid)

    return InstrumentationTrio(mainToProcess, processToMain)
}

fun signalChildReady(pid: Int) {
    processSyncDirectory.mkdirs()

    val signalFile = File(processSyncDirectory, "$pid.created")

    if (signalFile.exists()) {
        signalFile.delete()
    }

    val created = signalFile.createNewFile()

    if (!created) {
        throw IllegalStateException("cannot create signal file")
    }
}

fun <T> RdSignal<T>.init(lifetime: Lifetime, protocol: Protocol, latch: CountDownLatch): RdSignal<T> {
    return this.apply {
        async = true
        protocol.scheduler.invokeOrQueue {
            this.bind(lifetime, protocol, rdid.toString())
            latch.countDown()
        }
    }
}

data class InstrumentationTrio(
    val mainToChild: RdSignal<ByteArray>,
    val childToMain: RdSignal<ByteArray>
)