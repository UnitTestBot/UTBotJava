package org.utbot.framework.plugin.api.util

import java.io.OutputStream
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.DateFormat
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import mu.KotlinLogging
import org.utbot.framework.utbotHomePath

private val lockFilePath = "$utbotHomePath/utbot.lock"
private var currentLock : OutputStream? = null
private val logger = KotlinLogging.logger {}

object LockFile {
    @Synchronized
    fun isLocked() = currentLock != null

    @Synchronized
    fun lock(): Boolean {
        if (currentLock != null) return false
        return try {
            Paths.get(utbotHomePath).toFile().mkdirs()
            currentLock = Paths.get(lockFilePath).outputStream(StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE).also {
                it.write(DateFormat.getDateTimeInstance().format(System.currentTimeMillis()).toByteArray())
            }
            logger.debug("Locked")
            true
        } catch (e: Exception) {
            logger.error("Failed to lock")
            false
        }
    }

    @Synchronized
    fun unlock(): Boolean {
        try {
            val tmp = currentLock
            if (tmp != null) {
                tmp.close()
                Paths.get(lockFilePath).deleteIfExists()
                logger.debug("Unlocked")
                currentLock = null
                return true
            }
        } catch (ignored: Exception) {
            logger.error("Failed to unlock")
        }
        return false
    }
}