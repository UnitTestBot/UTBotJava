package org.utbot.framework.codegen.model.visitor

import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.codegen.model.UtilClassKind.Companion.UT_UTILS_PACKAGE_NAME
import org.utbot.framework.codegen.model.constructor.builtin.UtilMethodProvider
import org.utbot.framework.codegen.model.constructor.builtin.utUtilsClassId
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.MockFramework

/**
 * Information from [CgContext] that is relevant for the renderer.
 * Not all the information from [CgContext] is required to render a class,
 * so this more lightweight context is created for this purpose.
 */
internal class CgRendererContext(
    val shouldOptimizeImports: Boolean,
    val importedClasses: Set<ClassId>,
    val importedStaticMethods: Set<MethodId>,
    val classPackageName: String,
    val generatedClass: ClassId,
    val utilMethodProvider: UtilMethodProvider,
    val codegenLanguage: CodegenLanguage,
    val mockFrameworkUsed: Boolean,
    val mockFramework: MockFramework,
) {
    companion object {
        fun fromCgContext(context: CgContext): CgRendererContext {
            return CgRendererContext(
                shouldOptimizeImports = context.shouldOptimizeImports,
                importedClasses = context.importedClasses,
                importedStaticMethods = context.importedStaticMethods,
                classPackageName = context.testClassPackageName,
                generatedClass = context.outerMostTestClass,
                utilMethodProvider = context.utilMethodProvider,
                codegenLanguage = context.codegenLanguage,
                mockFrameworkUsed = context.mockFrameworkUsed,
                mockFramework = context.mockFramework
            )
        }

        fun fromUtilClassKind(utilClassKind: UtilClassKind, language: CodegenLanguage): CgRendererContext {
            return CgRendererContext(
                shouldOptimizeImports = false,
                importedClasses = emptySet(),
                importedStaticMethods = emptySet(),
                classPackageName = UT_UTILS_PACKAGE_NAME,
                generatedClass = utUtilsClassId,
                utilMethodProvider = utilClassKind.utilMethodProvider,
                codegenLanguage = language,
                mockFrameworkUsed = utilClassKind.mockFrameworkUsed,
                mockFramework = utilClassKind.mockFramework
            )
        }
    }
}