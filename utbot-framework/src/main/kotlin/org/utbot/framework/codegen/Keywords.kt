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
}

fun isLanguageKeyword(word: String, codegenLanguage: CodegenLanguage): Boolean =
    word in getLanguageKeywords(codegenLanguage)
