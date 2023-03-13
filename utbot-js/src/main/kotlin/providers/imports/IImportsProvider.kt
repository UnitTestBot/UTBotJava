package providers.imports

import service.PackageJson
import service.ServiceContext

interface IImportsProvider {

    val ternScriptImports: String

    val tempFileImports: String

    companion object {
        fun providerByPackageJson(packageJson: PackageJson, context: ServiceContext): IImportsProvider = when (packageJson.isModule) {
            true -> ModuleImportsProvider(context)
            else -> RequireImportsProvider(context)
        }
    }
}