package utils

import service.PackageJson

object ExportsProvider {

    fun getExportsRegex(packageJson: PackageJson): Regex = with(packageJson) {
        when (isModule) {
            true -> Regex("(.*)")
            false -> Regex("exports[.](.*) =")
        }
    }

    fun getExportsDelimiter(packageJson: PackageJson): String = with(packageJson) {
        when(isModule) {
            true -> ","
            false -> "\n"
        }
    }

    fun getExportsFrame(exportString: String, packageJson: PackageJson): String = with(packageJson) {
        when(isModule) {
            true -> exportString
            false -> "exports.$exportString = $exportString"
        }
    }

    fun getExportsPrefix(packageJson: PackageJson): String = with(packageJson) {
        when(isModule) {
            true -> "\nexport {"
            false -> "\n"
        }
    }

    fun getExportsPostfix(packageJson: PackageJson): String = with(packageJson) {
        when(isModule) {
            true -> "}\n"
            false -> "\n"
        }
    }
}