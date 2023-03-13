package providers.exports

class RequireExportsProvider : IExportsProvider {

    override val exportsDelimiter: String = "\n"

    override val exportsPostfix: String = "\n"

    override val exportsPrefix: String = "\n"

    override val exportsRegex: Regex = Regex("exports[.](.*) =")

    override fun getExportsFrame(exportString: String): String = "exports.$exportString = $exportString"

    override fun instrumentationFunExport(funName: String): String = "\nexports.$funName = $funName"
}