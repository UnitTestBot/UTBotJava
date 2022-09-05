package org.utbot.instrumentation.rd

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.lifetime.Lifetime
import org.utbot.common.utBotTempDirectory
import org.utbot.instrumentation.rd.generated.ProtocolModel
import org.utbot.instrumentation.rd.generated.protocolModel
import org.utbot.rd.pump
import java.io.File

const val rdProcessDirName = "rdProcessSync"
val processSyncDirectory = File(utBotTempDirectory.toFile(), rdProcessDirName)

internal suspend fun obtainClientIO(lifetime: Lifetime, protocol: Protocol): Pair<RdSignal<String>, ProtocolModel> {
    return protocol.scheduler.pump(lifetime) {
        val sync = RdSignal<String>().static(1).apply {
            async = true
            bind(lifetime, protocol, rdid.toString())
        }
        sync to protocol.protocolModel
    }
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