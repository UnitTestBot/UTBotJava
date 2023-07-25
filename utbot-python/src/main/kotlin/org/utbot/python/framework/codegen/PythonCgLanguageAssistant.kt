package org.utbot.python.framework.codegen

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.renderer.CgPrinter
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.renderer.CgRendererContext
import org.utbot.framework.codegen.services.language.AbstractCgLanguageAssistant
import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.codegen.model.constructor.name.PythonCgNameGenerator
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgCallableAccessManagerImpl
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgMethodConstructor
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgStatementConstructorImpl
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgVariableConstructor
import org.utbot.python.framework.codegen.model.constructor.visitor.CgPythonRenderer
import org.utbot.python.framework.codegen.model.services.access.PythonCgFieldStateManager

object PythonCgLanguageAssistant : AbstractCgLanguageAssistant() {
    override val extension: String
        get() = ".py"

    override val languageKeywords: Set<String> = setOf(
        "True", "False", "None", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
        "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
        "not", "or", "pass", "raise", "return", "try", "while", "with", "yield", "list", "int", "str", "float", "bool",
        "bytes", "frozenset", "dict", "set", "tuple", "abs", "aiter", "all", "any", "anext", "ascii", "bool",
        "breakpoint", "bytearray", "callable", "chr", "classmethod", "compile", "complex", "delattr", "dir", "divmod",
        "enumerate", "eval", "exec", "filter", "format", "getattr", "globals", "hasattr", "hash", "help", "hex", "id",
        "input", "isinstance", "issubclass", "iter", "len", "list", "locals", "map", "max", "memoryview", "min",
        "next", "object", "oct", "open", "ord", "pow", "print", "property", "range", "repr", "reversed", "round",
        "set", "setattr", "slice", "sorted", "staticmethod", "sum", "super", "type", "vars", "zip", "self"
    )

    override fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String> {
        val simpleName = testClassCustomName ?: "Test${classUnderTest.simpleName}"
        return Pair(simpleName, simpleName)
    }

    override fun getNameGeneratorBy(context: CgContext) = PythonCgNameGenerator(context)
    override fun getCallableAccessManagerBy(context: CgContext) = PythonCgCallableAccessManagerImpl(context)
    override fun getStatementConstructorBy(context: CgContext) = PythonCgStatementConstructorImpl(context)
    override fun getVariableConstructorBy(context: CgContext) = PythonCgVariableConstructor(context)
    override fun getMethodConstructorBy(context: CgContext) = PythonCgMethodConstructor(context)
    override fun getCgFieldStateManager(context: CgContext) = PythonCgFieldStateManager(context)
    override fun getLanguageTestFrameworkManager() = PythonTestFrameworkManager()
    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer =
        CgPythonRenderer(context, printer)

    var memoryObjects: MutableMap<Long, CgVariable> = emptyMap<Long, CgVariable>().toMutableMap()
    var memoryObjectsModels: MutableMap<Long, PythonTree.PythonTreeNode> = emptyMap<Long, PythonTree.PythonTreeNode>().toMutableMap()

    fun clear() {
        memoryObjects.clear()
        memoryObjectsModels.clear()
    }
}