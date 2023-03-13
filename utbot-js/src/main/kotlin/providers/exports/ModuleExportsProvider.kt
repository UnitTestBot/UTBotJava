package providers.exports

class ModuleExportsProvider : IExportsProvider {

    override val exportsDelimiter: String = ","

    override val exportsPostfix: String = "}\n"

    override val exportsPrefix: String = "\nexport {"

    override val exportsRegex: Regex = Regex("(.*)")

    override fun getExportsFrame(exportString: String): String = exportString

    override fun instrumentationFunExport(funName: String): String = "\nexport {$funName}"
}