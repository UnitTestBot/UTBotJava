package org.utbot.framework.codegen.tree.ututils

import org.utbot.framework.codegen.domain.builtin.selectUtilClassId
import org.utbot.framework.codegen.domain.models.CgAuxiliaryClass
import org.utbot.framework.codegen.domain.models.CgAuxiliaryNestedClassesRegion
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgStaticsRegion
import org.utbot.framework.codegen.domain.models.CgUtilMethod
import org.utbot.framework.codegen.tree.buildClass
import org.utbot.framework.codegen.tree.buildClassBody
import org.utbot.framework.codegen.tree.buildClassFile
import org.utbot.framework.plugin.api.CodegenLanguage

/**
 * This class is used to construct a file containing an util class UtUtils.
 * The util class is constructed when the argument `generateUtilClassFile` in the [CodeGenerator] is true.
 */
internal object CgUtilClassConstructor {
    fun constructUtilsClassFile(
        utilClassKind: UtilClassKind,
        codegenLanguage: CodegenLanguage,
    ): CgClassFile {
        val utilMethodProvider = utilClassKind.utilMethodProvider
        val utilsClassId = selectUtilClassId(codegenLanguage)
        return buildClassFile {
            // imports are empty, because we use fully qualified classes and static methods,
            // so they will be imported once IDEA reformatting action has worked
            declaredClass = buildClass {
                id = utilsClassId
                documentation = utilClassKind.utilClassDocumentation(codegenLanguage)
                body = buildClassBody(utilsClassId) {
                    staticDeclarationRegions += CgStaticsRegion(
                        header = "Util methods",
                        content = utilMethodProvider.utilMethodIds.map { CgUtilMethod(it) })

                    nestedClassRegions += CgAuxiliaryNestedClassesRegion(
                        header = "Util classes",
                        nestedClasses = listOf(
                            CgAuxiliaryClass(utilMethodProvider.capturedArgumentClassId)
                        )
                    )
                }
            }
        }
    }
}