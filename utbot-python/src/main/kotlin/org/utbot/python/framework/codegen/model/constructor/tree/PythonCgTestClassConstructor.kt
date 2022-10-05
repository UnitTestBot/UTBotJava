package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.tree.*

internal class PythonCgTestClassConstructor(context: CgContext) : CgTestClassConstructor(context) {
    override fun construct(testClassModel: TestClassModel): CgTestClassFile {
        return buildTestClassFile {
            this.declaredClass = withTestClassScope { constructTestClass(testClassModel) }
            imports.addAll(context.collectedImports)
            testsGenerationReport = this@PythonCgTestClassConstructor.testsGenerationReport
        }
    }
}
