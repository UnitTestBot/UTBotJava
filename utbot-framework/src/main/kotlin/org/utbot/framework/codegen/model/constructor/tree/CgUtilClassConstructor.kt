package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.codegen.model.constructor.builtin.utJavaUtilsClassId
import org.utbot.framework.codegen.model.constructor.builtin.utKotlinUtilsClassId
import org.utbot.framework.codegen.model.tree.CgAuxiliaryClass
import org.utbot.framework.codegen.model.tree.CgAuxiliaryNestedClassesRegion
import org.utbot.framework.codegen.model.tree.CgClassFile
import org.utbot.framework.codegen.model.tree.CgStaticsRegion
import org.utbot.framework.codegen.model.tree.CgUtilMethod
import org.utbot.framework.codegen.model.tree.buildClass
import org.utbot.framework.codegen.model.tree.buildClassBody
import org.utbot.framework.codegen.model.tree.buildClassFile
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
        val utilsClassId = when (codegenLanguage) {
            CodegenLanguage.JAVA -> utJavaUtilsClassId
            CodegenLanguage.KOTLIN -> utKotlinUtilsClassId
        }
        return buildClassFile {
            // imports are empty, because we use fully qualified classes and static methods,
            // so they will be imported once IDEA reformatting action has worked
            declaredClass = buildClass {
                id = utilsClassId
                body = buildClassBody(utilsClassId) {
                    documentation = utilClassKind.utilClassDocumentation(codegenLanguage)
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