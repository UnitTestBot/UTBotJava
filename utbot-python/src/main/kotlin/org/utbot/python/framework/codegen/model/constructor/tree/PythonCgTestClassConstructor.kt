package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.tree.CgSimpleTestClassConstructor
import org.utbot.framework.codegen.tree.buildClassFile

internal class PythonCgTestClassConstructor(context: CgContext) : CgSimpleTestClassConstructor(context) {
    override fun construct(testClassModel: TestClassModel): CgClassFile {
        return buildClassFile {
            this.declaredClass = withTestClassScope {
                with(currentTestClassContext) { testClassSuperclass = testFramework.testSuperClass }
                constructTestClass(testClassModel)
            }
            imports.addAll(context.collectedImports)
        }
    }
}
