package framework.codegen

import framework.api.ts.TsClassId
import framework.api.ts.util.tsErrorClassId
import framework.api.ts.util.tsUndefinedClassId
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId


object Mocha : TestFramework(id = "Mocha", displayName = "Mocha") {
    override val mainPackage = ""
    override val assertionsClass = tsUndefinedClassId
    override val arraysAssertionsClass = tsUndefinedClassId
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
        get() = tsUndefinedClassId

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

    override val parameterizedTestAnnotationId = tsUndefinedClassId
    override val methodSourceAnnotationId = tsUndefinedClassId
}

internal val tsAssertEquals by lazy {
    BuiltinMethodId(
        TsClassId("assert.deepEqual"), "assert.deepEqual", tsUndefinedClassId, listOf(
            tsUndefinedClassId, tsUndefinedClassId
        )
    )
}

internal val tsAssertThrows by lazy {
    BuiltinMethodId(
        TsClassId("assert.throws"), "assert.throws", tsErrorClassId, listOf(
            tsUndefinedClassId, tsUndefinedClassId, tsUndefinedClassId
        )
    )
}
