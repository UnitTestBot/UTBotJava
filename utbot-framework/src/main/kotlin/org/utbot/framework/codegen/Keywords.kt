package org.utbot.framework.codegen

import org.utbot.framework.plugin.api.CodegenLanguage

private val javaKeywords = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto",
    "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
    "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
    "throw", "throws", "transient", "try", "void", "volatile", "while", "null", "false", "true"
)

private val kotlinHardKeywords = setOf(
    "as", "as?", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "!in", "interface",
    "is", "!is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
    "val", "var", "when", "while"
)

private val pythonKeywords = setOf(
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

@Suppress("unused")
private val kotlinSoftKeywords = setOf(
    "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import", "init",
    "param", "property", "receiver", "set", "setparam", "value", "where"
)

@Suppress("unused")
private val kotlinModifierKeywords = setOf(
    "actual", "abstract", "annotation", "companion", "const", "crossinline", "data", "enum", "expect", "external",
    "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator", "out", "override",
    "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg"
)

// For now we check only hard keywords because others can be used as methods and variables identifiers
private val kotlinKeywords = kotlinHardKeywords

private fun getLanguageKeywords(codegenLanguage: CodegenLanguage): Set<String> = when(codegenLanguage) {
    CodegenLanguage.JAVA -> javaKeywords
    CodegenLanguage.KOTLIN -> kotlinKeywords
    CodegenLanguage.PYTHON -> pythonKeywords
}

fun isLanguageKeyword(word: String, codegenLanguage: CodegenLanguage): Boolean =
    word in getLanguageKeywords(codegenLanguage)
