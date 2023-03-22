package org.utbot.go.imports

import org.utbot.go.api.util.getAllNamedTypes
import org.utbot.go.framework.api.go.GoImport
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId

object GoImportsResolver {

    fun resolveImportsBasedOnTypes(
        types: List<GoTypeId>,
        sourcePackage: GoPackage,
        busyImports: Set<GoImport> = emptySet()
    ): Set<GoImport> {
        val namedTypes = types.getAllNamedTypes()
        val result = busyImports.toMutableSet()
        val busyAliases = busyImports.map { it.alias ?: it.goPackage.packageName }.toMutableSet()
        namedTypes.map { it.sourcePackage }.distinct().filter { it != sourcePackage }.forEach { goPackage ->
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
            result += GoImport(goPackage, alias)
        }
        return result
    }

    fun resolveImportsBasedOnRequiredPackages(
        requiredPackages: Set<GoPackage>,
        sourcePackage: GoPackage,
        busyImports: Set<GoImport> = emptySet()
    ): Set<GoImport> {
        val result = busyImports.toMutableSet()
        val busyAliases = busyImports.map { it.alias ?: it.goPackage.packageName }.toMutableSet()
        requiredPackages.distinct().filter { it != sourcePackage }.forEach { goPackage ->
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
            result += GoImport(goPackage, alias)
        }
        return result
    }
}