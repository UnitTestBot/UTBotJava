package org.utbot.framework.codegen.services.language

fun isLanguageKeyword(word: String, codegenLanguageAssistant: CgLanguageAssistant): Boolean =
    word in codegenLanguageAssistant.languageKeywords
