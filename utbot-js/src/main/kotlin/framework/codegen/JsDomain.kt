package framework.codegen

import framework.api.js.JsClassId
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.jsUndefinedClassId
import org.utbot.framework.codegen.domain.Import
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import service.PackageJson


object Mocha : TestFramework(id = "Mocha", displayName = "Mocha") {
    override val mainPackage = ""
    override val assertionsClass = jsUndefinedClassId
    override val arraysAssertionsClass = jsUndefinedClassId
    override val kotlinFailureAssertionsClass: ClassId = jsUndefinedClassId

    override val beforeMethodId: ClassId = jsUndefinedClassId
    override val afterMethodId: ClassId = jsUndefinedClassId

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

enum class ModuleType {
    MODULE,
    COMMONJS;

    companion object {
        fun fromPackageJson(packageJson: PackageJson): ModuleType {
            return when (packageJson.isModule) {
                true -> MODULE
                else -> COMMONJS
            }
        }
    }
}

data class JsImport(
    val name: String,
    val aliases: String,
    val path: String,
    val type: ModuleType
): Import(2) {

    override val qualifiedName: String = "$name as $aliases from $path"
}
