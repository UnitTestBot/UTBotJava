package utils

import service.PackageJson

object ExportsProvider {

    fun getExportsRegex(packageJson: PackageJson): Regex = with(packageJson) {
        when (isModule) {
            true -> Regex("")
            false -> Regex("exports[.](.*) =")
        }
    }

    fun getExportsDelimiter(packageJson: PackageJson): String = with(packageJson) {
        when(isModule) {
            true -> ","
            false -> "\n"
        }
    }
}