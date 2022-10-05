package org.utbot.python.framework.codegen

import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenLanguage
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.framework.codegen.model.constructor.name.PythonCgNameGenerator
import org.utbot.python.framework.codegen.model.constructor.tree.*
import org.utbot.python.framework.codegen.model.constructor.visitor.CgPythonRenderer

object PythonCodeLanguage : CodeGenLanguage() {
    override val displayName: String = "Python"
    override val id: String = "Python"

    override val extension: String
        get() = ".py"

    override val languageKeywords: Set<String> = setOf(
        "True", "False", "None", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else",
        "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not",
        "or", "pass", "raise", "return", "try", "while", "with", "yield", "list", "int", "str", "float", "bool", "bytes", "frozenset",
        "dict", "set", "tuple",
        "abs", "aiter", "all", "any", "anext", "ascii", "bool", "breakpoint", "bytearray", "callable", "chr", "classmethod", "compile",
        "complex", "delattr", "dir", "divmod", "enumerate", "eval", "exec", "filter", "format", "getattr", "globals", "hasattr",
        "hash", "help", "hex", "id", "input", "isinstance", "issubclass", "iter", "len", "list", "locals", "map", "max",
        "memoryview", "min", "next", "object", "oct", "open", "ord", "pow", "print", "property", "range", "repr", "reversed",
        "round", "set", "setattr", "slice", "sorted", "staticmethod", "sum", "super", "type", "vars", "zip", "self"
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
    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer = CgPythonRenderer(context, printer)

    override val testFrameworks = listOf(Unittest, Pytest)

    override fun managerByFramework(context: CgContext): TestFrameworkManager = when (context.testFramework) {
        is Unittest -> UnittestManager(context)
        is Pytest -> PytestManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework = Unittest

}