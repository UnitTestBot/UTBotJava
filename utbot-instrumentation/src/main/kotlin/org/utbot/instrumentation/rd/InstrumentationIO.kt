package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.lifetime.Lifetime
import org.utbot.common.utBotTempDirectory
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

const val rdProcessDirName = "rdProcessSync"
val processSyncDirectory = File(utBotTempDirectory.toFile(), rdProcessDirName)
private val awaitTimeoutMillis: Long = 120 * 1000

internal fun obtainClientIO(lifetime: Lifetime, protocol: Protocol, pid: Int): InstrumentationIO {
    val latch = CountDownLatch(3)
    val mainToProcess = RdSignal<ByteArray>().static(1).init(lifetime, protocol, latch)
    val processToMain = RdSignal<ByteArray>().static(2).init(lifetime, protocol, latch)
    val sync = RdSignal<String>().static(3).init(lifetime, protocol, latch)

    if (!latch.await(awaitTimeoutMillis, TimeUnit.MILLISECONDS))
        throw IllegalStateException("Cannot bind signals")

    signalChildReady(pid)

    return InstrumentationIO(mainToProcess, processToMain, sync)
}

internal fun childCreatedFileName(pid: Int): String {
    return "$pid.created"
}

internal fun signalChildReady(pid: Int) {
    processSyncDirectory.mkdirs()

    val signalFile = File(processSyncDirectory, childCreatedFileName(pid))

    if (signalFile.exists()) {
        signalFile.delete()
    }

    val created = signalFile.createNewFile()

    if (!created) {
        throw IllegalStateException("cannot create signal file")
    }
}

private fun <T> RdSignal<T>.init(lifetime: Lifetime, protocol: Protocol, latch: CountDownLatch): RdSignal<T> {
    return this.apply {
        async = true
        protocol.scheduler.invokeOrQueue {
            this.bind(lifetime, protocol, rdid.toString())
            latch.countDown()
        }
    }
}

internal data class InstrumentationIO(
    val mainToChild: RdSignal<ByteArray>,
    val childToMain: RdSignal<ByteArray>,
    val sync: RdSignal<String>
)