package org.utbot.python.framework.codegen.model

import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.python.PythonClassId
import org.utbot.framework.plugin.api.python.pythonAnyClassId
import org.utbot.framework.plugin.api.python.pythonNoneClassId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.voidClassId

object Pytest : TestFramework(displayName = "pytest") {
    override val mainPackage: String = "pytest"
    override val assertionsClass: ClassId = pythonNoneClassId
    override val arraysAssertionsClass: ClassId = assertionsClass
    override val testAnnotation: String
        get() = TODO("Not yet implemented")
    override val testAnnotationId: ClassId = BuiltinClassId(
        name = "pytest",
        canonicalName = "pytest",
        simpleName = "Tests"
    )
    override val testAnnotationFqn: String = ""

    override val parameterizedTestAnnotation: String = ""
    override val parameterizedTestAnnotationId: ClassId = pythonAnyClassId
    override val parameterizedTestAnnotationFqn: String = ""
    override val methodSourceAnnotation: String = ""
    override val methodSourceAnnotationId: ClassId = pythonAnyClassId
    override val methodSourceAnnotationFqn: String = ""
    override val nestedClassesShouldBeStatic: Boolean = false
    override val argListClassId: ClassId = pythonAnyClassId

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

object Unittest : TestFramework(displayName = "Unittest") {
    override val testSuperClass: ClassId = PythonClassId("unittest.TestCase")
    override val mainPackage: String = "unittest"
    override val assertionsClass: ClassId = PythonClassId("self")
    override val arraysAssertionsClass: ClassId = assertionsClass
    override val testAnnotation: String = ""
    override val testAnnotationId: ClassId = BuiltinClassId(
        name = "Unittest",
        canonicalName = "Unittest",
        simpleName = "Tests"
    )
    override val testAnnotationFqn: String = "unittest"

    override val parameterizedTestAnnotation: String = ""
    override val parameterizedTestAnnotationId: ClassId = pythonAnyClassId
    override val parameterizedTestAnnotationFqn: String = ""
    override val methodSourceAnnotation: String = ""
    override val methodSourceAnnotationId: ClassId = pythonAnyClassId
    override val methodSourceAnnotationFqn: String = ""
    override val nestedClassesShouldBeStatic: Boolean = false
    override val argListClassId: ClassId = pythonAnyClassId

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

