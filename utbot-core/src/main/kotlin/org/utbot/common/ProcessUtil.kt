package org.utbot.common

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT

/**
 * working pid for jvm 8 and 9+
 */
val Process.getPid: Long
    get() = try {
        if (isJvm9Plus) {
            // because we cannot reference Java9+ API here
            ClassLoader.getSystemClassLoader().loadClass("java.lang.Process").getDeclaredMethod("pid").invoke(this) as Long
        } else {
            when (javaClass.name) {
                "java.lang.UNIXProcess" -> {
                    val fPid = javaClass.getDeclaredField("pid")
                    fPid.withAccessibility { fPid.getLong(this) }

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

                else -> -2
            }
        }
    } catch (e: Exception) {
        -1
    }

private interface CLibrary : Library {
    fun getpid(): Int

    companion object {
        val INSTANCE = Native.load("c", CLibrary::class.java) as CLibrary
    }
}

/**
 * working for jvm 8 and 9+
 */
val currentProcessPid: Long
    get() =
        try {
            if (isJvm9Plus) {
                ClassLoader.getSystemClassLoader().loadClass("java.lang.ProcessHandle").let {
                    val handle = it.getDeclaredMethod("current").invoke(it)
                    it.getDeclaredMethod("pid").invoke(handle) as Long
                }
            } else {
                if (isWindows) {
                    Kernel32.INSTANCE.GetCurrentProcessId()
                } else {
                    CLibrary.INSTANCE.getpid()
                }.toLong()
            }
        } catch (e: Throwable) {
            -1
        }