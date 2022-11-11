package framework.codegen

import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import framework.api.js.JsClassId
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.jsUndefinedClassId


object Mocha : TestFramework(id = "Mocha", displayName = "Mocha") {
    override val mainPackage = ""
    override val assertionsClass = jsUndefinedClassId
    override val arraysAssertionsClass = jsUndefinedClassId
    override val testAnnotation = ""
    override val testAnnotationFqn = ""

    override val parameterizedTestAnnotation: String
        get() = throw UnsupportedOperationException("Parameterized tests are not supported for Mocha")
    override val parameterizedTestAnnotationFqn: String
        get() = throw UnsupportedOperationException("Parameterized tests are not supported for Mocha")
    override val methodSourceAnnotation: String
        get() = throw UnsupportedOperationException("Parameterized tests are not supported for Mocha")
    override val methodSourceAnnotationFqn: String
        get() = throw UnsupportedOperationException("Parameterized tests are not supported for Mocha")

    override val nestedClassesShouldBeStatic: Boolean
        get() = false
    override val argListClassId: ClassId
        get() = jsUndefinedClassId

    override fun getRunTestsCommand(
        executionInvoke: String,
        classPath: String,
        classesNames: List<String>,
        buildDirectory: String,
        additionalArguments: List<String>
    ): List<String> {
        throw UnsupportedOperationException()
    }

    override val testAnnotationId = BuiltinClassId(
        name = "Mocha",
        canonicalName = "Mocha",
        simpleName = "Test"
    )

    override val parameterizedTestAnnotationId = jsUndefinedClassId
    override val methodSourceAnnotationId = jsUndefinedClassId
}

internal val jsAssertEquals by lazy {
    BuiltinMethodId(
        JsClassId("assert.deepEqual"), "assert.deepEqual", jsUndefinedClassId, listOf(
            jsUndefinedClassId, jsUndefinedClassId
        )
    )
}

internal val jsAssertThrows by lazy {
    BuiltinMethodId(
        JsClassId("assert.throws"), "assert.throws", jsErrorClassId, listOf(
            jsUndefinedClassId, jsUndefinedClassId, jsUndefinedClassId
        )
    )
}
