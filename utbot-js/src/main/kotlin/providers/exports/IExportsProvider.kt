package providers.exports

import service.PackageJson

interface IExportsProvider {

    val exportsRegex: Regex

    val exportsDelimiter: String

    fun getExportsFrame(exportString: String): String

    val exportsPrefix: String

    val exportsPostfix: String

    fun instrumentationFunExport(funName: String): String

    companion object {
        fun providerByPackageJson(packageJson: PackageJson): IExportsProvider = when (packageJson.isModule) {
            true -> ModuleExportsProvider()
            else -> RequireExportsProvider()
        }
    }
}