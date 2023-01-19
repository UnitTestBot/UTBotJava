package utils

import java.util.Locale

abstract class OsProvider {

    abstract fun getCmdPrefix(): Array<String>
    abstract fun getAbstractivePathTool(): String

    abstract val npmPackagePostfix: String

    companion object {

        fun getProviderByOs(): OsProvider {
            val osData = System.getProperty("os.name").lowercase(Locale.getDefault())
            return when {
                osData.contains("windows") -> WindowsProvider()
                else -> LinuxProvider()
            }
        }
    }
}

class WindowsProvider : OsProvider() {
    override fun getCmdPrefix() = emptyArray<String>()
    override fun getAbstractivePathTool() = "where"

    override val npmPackagePostfix = ".cmd"
}

class LinuxProvider : OsProvider() {
    override fun getCmdPrefix() = emptyArray<String>()
    override fun getAbstractivePathTool() = "which"

    override val npmPackagePostfix = ""
}
