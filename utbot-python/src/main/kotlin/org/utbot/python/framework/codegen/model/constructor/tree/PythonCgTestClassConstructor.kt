package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgTestClassFile
import org.utbot.framework.codegen.tree.CgTestClassConstructor
import org.utbot.framework.codegen.tree.buildTestClassFile

internal class PythonCgTestClassConstructor(context: CgContext) : CgTestClassConstructor(context) {
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
