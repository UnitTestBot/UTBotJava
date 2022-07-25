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

    fun kill(pid: Int, signal: Int): Int

    companion object {
        val INSTANCE = Native.load("c", CLibrary::class.java) as CLibrary
    }
}

private val os = System.getProperty("os.name").toLowerCase()
val isWindows = os.startsWith("windows")
val isUnix = !isWindows
val isMac = os.startsWith("mac")

val currentProcessPid: Long get() = try {
    if (isWindows) {
        Kernel32.INSTANCE.GetCurrentProcessId()
    } else {
        CLibrary.INSTANCE.getpid()
    }
}
catch (e: Throwable) {
    -1
}.toLong()

fun Process.kill(killChildren: Boolean = false): Boolean {
    try {
        val id = pid

        if (isWindows) {
            val taskkill = listOf("taskkill", "/f", "/pid", id.toString(), if (killChildren) "/t" else "")

            ProcessBuilder(taskkill).start().waitFor()
        } else {
            if (killChildren) {
                TODO("this is hard at java8, need /bin/ps with format, different arguments for linux & macos, output parsing\n" +
                        "at java 9+ we have ProcessHandle that can return process list and process descendants\n" +
                        "hope this won't be needed until we move to java9+")
            }
            CLibrary.INSTANCE.kill(id.toInt(), 9)
        }

        return isAlive
    }
    catch (e: Throwable) {
        return false
    }
}

fun getCurrentProcessId() =
    try {
        ManagementFactory.getRuntimeMXBean()?.let {
            it.name.split("@")[0].toLong()
        } ?: -1
    } catch (t: Throwable) {
        -1
    }