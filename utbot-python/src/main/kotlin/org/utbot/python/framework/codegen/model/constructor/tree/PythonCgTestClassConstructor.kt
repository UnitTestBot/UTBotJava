package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.tree.CgClassFile
import org.utbot.framework.codegen.model.tree.buildClassFile

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
