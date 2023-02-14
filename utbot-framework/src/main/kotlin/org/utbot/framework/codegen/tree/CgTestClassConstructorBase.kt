package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.services.CgNameGenerator
import org.utbot.framework.codegen.services.framework.TestFrameworkManager

abstract class CgTestClassConstructorBase<T : TestClassModel>(val context: CgContext):
    CgContextOwner by context,
    CgStatementConstructor by CgComponents.getStatementConstructorBy(context){

    protected abstract val methodConstructor: CgMethodConstructor
    protected open val nameGenerator: CgNameGenerator = CgComponents.getNameGeneratorBy(context)
    protected open val testFrameworkManager: TestFrameworkManager = CgComponents.getTestFrameworkManagerBy(context)

    /**
     * Constructs a file with the test class corresponding to [SimpleTestClassModel].
     */
    open fun construct(testClassModel: T): CgClassFile {
        return buildClassFile {
            this.declaredClass = withTestClassScope { constructTestClass(testClassModel) }
            imports += context.collectedImports
        }
    }

    /**
     * Constructs [CgClass] corresponding to [SimpleTestClassModel].
     */
    open fun constructTestClass(testClassModel: T): CgClass {
        return buildClass {
            id = currentTestClass

            if (currentTestClass != outerMostTestClass) {
                isNested = true
                isStatic = testFramework.nestedClassesShouldBeStatic
                testFrameworkManager.annotationForNestedClasses?.let {
                    currentTestClassContext.collectedTestClassAnnotations += it
                }
            }

            body = constructTestClassBody(testClassModel)

            // It is important that annotations, superclass and interfaces assignment is run after
            // all methods are generated so that all necessary info is already present in the context
            with (currentTestClassContext) {
                annotations += collectedTestClassAnnotations
                superclass = testClassSuperclass
                interfaces += collectedTestClassInterfaces
            }
        }
    }

    abstract fun constructTestClassBody(testClassModel: T): CgClassBody
}