package org.utbot.go.imports

import org.utbot.go.api.util.getAllVisibleNamedTypes
import org.utbot.go.framework.api.go.GoImport
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId

object GoImportsResolver {

    fun resolveImportsBasedOnTypes(
        types: List<GoTypeId>,
        sourcePackage: GoPackage,
        busyImports: Set<GoImport> = emptySet()
    ): Set<GoImport> = resolveImportsBasedOnRequiredPackages(
        types.getAllVisibleNamedTypes(sourcePackage).map { it.sourcePackage }.toSet(), sourcePackage, busyImports
    )

    fun resolveImportsBasedOnRequiredPackages(
        requiredPackages: Set<GoPackage>,
        sourcePackage: GoPackage,
        busyImports: Set<GoImport> = emptySet()
    ): Set<GoImport> {
        val result = busyImports.associateBy { it.goPackage }.toMutableMap()
        val busyAliases = busyImports.map { it.alias ?: it.goPackage.packageName }.toMutableSet()
        requiredPackages.distinct().filter { it != sourcePackage && !it.isBuiltin && it !in result }
            .forEach { goPackage ->
                val alias = if (goPackage.packageName in busyAliases) {
                    var n = 1
                    while (goPackage.packageName + n in busyAliases) {
                        n++
                    }
                    goPackage.packageName + n
                } else {
                    null
                }
                busyAliases += alias ?: goPackage.packageName
                result[goPackage] = GoImport(goPackage, alias)
            }
        return result.values.toSet()
    }
}