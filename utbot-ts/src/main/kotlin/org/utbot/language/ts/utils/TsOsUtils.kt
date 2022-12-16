package org.utbot.language.ts.utils

import java.util.Locale

abstract class TsOsProvider {

    abstract fun getCmdPrefix(): Array<String>
    abstract fun getAbstractivePathTool(): String

    companion object {

        fun getProviderByOs(): TsOsProvider {
            val osData = System.getProperty("os.name").lowercase(Locale.getDefault())
            return when {
                osData.contains("windows") -> WindowsProvider()
                else -> LinuxProvider()
            }
        }
    }
}

class WindowsProvider : TsOsProvider() {
    override fun getCmdPrefix() = arrayOf("cmd.exe", "/c")
    override fun getAbstractivePathTool() = "where"
}

class LinuxProvider : TsOsProvider() {
    override fun getCmdPrefix() = emptyArray<String>()
    override fun getAbstractivePathTool() = "which"
}
