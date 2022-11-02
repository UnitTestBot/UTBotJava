package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.codegen.model.constructor.builtin.utUtilsClassId
import org.utbot.framework.codegen.model.tree.CgAuxiliaryClass
import org.utbot.framework.codegen.model.tree.CgAuxiliaryNestedClassesRegion
import org.utbot.framework.codegen.model.tree.CgClassFile
import org.utbot.framework.codegen.model.tree.CgStaticsRegion
import org.utbot.framework.codegen.model.tree.CgUtilMethod
import org.utbot.framework.codegen.model.tree.buildClass
import org.utbot.framework.codegen.model.tree.buildClassBody
import org.utbot.framework.codegen.model.tree.buildClassFile

/**
 * This class is used to construct a file containing an util class UtUtils.
 * The util class is constructed when the argument `generateUtilClassFile` in the [CodeGenerator] is true.
 */
internal object CgUtilClassConstructor {
    fun constructUtilsClassFile(utilClassKind: UtilClassKind): CgClassFile {
        val utilMethodProvider = utilClassKind.utilMethodProvider
        return buildClassFile {
            // imports are empty, because we use fully qualified classes and static methods,
            // so they will be imported once IDEA reformatting action has worked
            declaredClass = buildClass {
                id = utUtilsClassId
                body = buildClassBody(utUtilsClassId) {
                    documentation = utilClassKind.utilClassDocumentation
                    staticDeclarationRegions += CgStaticsRegion("Util methods", utilMethodProvider.utilMethodIds.map { CgUtilMethod(it) })
                    nestedClassRegions += CgAuxiliaryNestedClassesRegion(
                        nestedClasses = listOf(
                            CgAuxiliaryClass(utilMethodProvider.capturedArgumentClassId)
                        )
                    )
                }
            }
        }
    }
}