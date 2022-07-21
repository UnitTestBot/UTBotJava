package org.utbot.common

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import java.lang.management.ManagementFactory

val Process.pid : Long get() = try {
    when (javaClass.name) {
        "java.lang.UNIXProcess" -> {
            val fPid = javaClass.getDeclaredField("pid")
            fPid.withAccessibility { fPid.getLong(this)  }

        }
        "java.lang.Win32Process", "java.lang.ProcessImpl" -> {
            val fHandle = javaClass.getDeclaredField("handle")
            fHandle.withAccessibility {
                val handle = fHandle.getLong(this)
                val winntHandle = WinNT.HANDLE()
                winntHandle.pointer = Pointer.createConstant(handle)
                Kernel32.INSTANCE.GetProcessId(winntHandle).toLong()
            }
        }
        else -> -1
    }
} catch (e: Exception) { -2 }

private interface CLibrary : Library {
    fun getpid(): Int

    companion object {
        val INSTANCE = Native.loadLibrary("c", CLibrary::class.java) as CLibrary
    }
}

private val os = System.getProperty("os.name").toLowerCase()
val isWindows = os.startsWith("windows")
val isUnix = !isWindows
val isMacOs = os.startsWith("mac")

val currentProcessPid: Int get() = try {
    if (isWindows) {
        Kernel32.INSTANCE.GetCurrentProcessId()
    } else {
        CLibrary.INSTANCE.getpid()
    }
}
catch (e: Throwable) {
    -1
}

fun getCurrentProcessId() =
    try {
        ManagementFactory.getRuntimeMXBean()?.let {
            it.name.split("@")[0].toLong()
        } ?: -1
    } catch (t: Throwable) {
        -1
    }