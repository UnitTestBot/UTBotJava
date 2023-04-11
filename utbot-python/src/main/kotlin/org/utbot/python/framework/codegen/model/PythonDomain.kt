package org.utbot.python.framework.codegen.model

import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId

object Pytest : TestFramework(displayName = "pytest", id = "pytest") {
    override val mainPackage: String = "pytest"
    override val assertionsClass: ClassId = pythonNoneClassId
    override val arraysAssertionsClass: ClassId = assertionsClass
    override val kotlinFailureAssertionsClass: ClassId = assertionsClass
    override val testAnnotation: String
        get() = TODO("Not yet implemented")
    override val testAnnotationId: ClassId = BuiltinClassId(
        canonicalName = "pytest",
        simpleName = "Tests"
    )
    override val testAnnotationFqn: String = ""

    override val beforeMethod: String = ""
    override val beforeMethodId: ClassId = pythonAnyClassId
    override val beforeMethodFqn: String = ""

    override val afterMethod: String = ""
    override val afterMethodId: ClassId = pythonAnyClassId
    override val afterMethodFqn: String = ""

    override val parameterizedTestAnnotation: String = ""
    override val parameterizedTestAnnotationId: ClassId = pythonAnyClassId
    override val parameterizedTestAnnotationFqn: String = ""
    override val methodSourceAnnotation: String = ""
    override val methodSourceAnnotationId: ClassId = pythonAnyClassId
    override val methodSourceAnnotationFqn: String = ""
    override val nestedClassesShouldBeStatic: Boolean = false
    override val argListClassId: ClassId = pythonAnyClassId

    val skipDecoratorClassId = PythonClassId("pytest", "mark.skip")

    @OptIn(ExperimentalStdlibApi::class)
    override fun getRunTestsCommand(
        executionInvoke: String,
        classPath: String,
        classesNames: List<String>,
        buildDirectory: String,
        additionalArguments: List<String>
    ): List<String> = buildList {
        add(executionInvoke)
        addAll(additionalArguments)
        add(mainPackage)
    }
}

object Unittest : TestFramework(displayName = "Unittest", id = "Unittest") {
    init {
        isInstalled = true
    }

    override val testSuperClass: ClassId = PythonClassId("unittest.TestCase")
    override val mainPackage: String = "unittest"
    override val assertionsClass: ClassId = PythonClassId("self")
    override val arraysAssertionsClass: ClassId = assertionsClass
    override val kotlinFailureAssertionsClass: ClassId = assertionsClass
    override val testAnnotation: String = ""
    override val testAnnotationId: ClassId = BuiltinClassId(
        canonicalName = "Unittest",
        simpleName = "Tests"
    )
    override val testAnnotationFqn: String = "unittest"

    override val beforeMethod: String = ""
    override val beforeMethodId: ClassId = pythonAnyClassId
    override val beforeMethodFqn: String = ""

    override val afterMethod: String = ""
    override val afterMethodId: ClassId = pythonAnyClassId
    override val afterMethodFqn: String = ""

    override val parameterizedTestAnnotation: String = ""
    override val parameterizedTestAnnotationId: ClassId = pythonAnyClassId
    override val parameterizedTestAnnotationFqn: String = ""
    override val methodSourceAnnotation: String = ""
    override val methodSourceAnnotationId: ClassId = pythonAnyClassId
    override val methodSourceAnnotationFqn: String = ""
    override val nestedClassesShouldBeStatic: Boolean = false
    override val argListClassId: ClassId = pythonAnyClassId

    val skipDecoratorClassId = PythonClassId("unittest.skip")

    override fun getRunTestsCommand(
        executionInvoke: String,
        classPath: String,
        classesNames: List<String>,
        buildDirectory: String,
        additionalArguments: List<String>
    ): List<String> {
        throw UnsupportedOperationException()
    }

    override val assertEquals by lazy { assertionId("assertEqual", objectClassId, objectClassId) }

    override fun assertionId(name: String, vararg params: ClassId): MethodId =
        methodId(assertionsClass, name, voidClassId, *params)
}

