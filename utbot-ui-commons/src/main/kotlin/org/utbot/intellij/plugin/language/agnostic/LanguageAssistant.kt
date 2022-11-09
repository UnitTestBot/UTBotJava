package org.utbot.intellij.plugin.language.agnostic

import mu.KotlinLogging
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

private val logger = KotlinLogging.logger {}

abstract class LanguageAssistant {

    abstract fun update(e: AnActionEvent)
    abstract fun actionPerformed(e: AnActionEvent)

    companion object {
        private val languages = mutableMapOf<String, LanguageAssistant>()

        fun get(e: AnActionEvent): LanguageAssistant? {
            e.getData(CommonDataKeys.PSI_FILE)?.language?.let { language ->
                if (!languages.containsKey(language.id)) {
                    loadWithException(language)?.let {
                        languages.put(language.id, it.kotlin.objectInstance as LanguageAssistant)
                    }
                }
                return languages[language.id]
            }
            return null
        }
    }
}

private fun loadWithException(language: Language): Class<*>? {
    try {
        return when (language.id) {
            "Python" -> Class.forName("org.utbot.intellij.plugin.language.python.PythonLanguageAssistant")
            "ECMAScript 6" -> Class.forName("org.utbot.intellij.plugin.language.js.JsLanguageAssistant")
            "JAVA", "kotlin" -> Class.forName("org.utbot.intellij.plugin.language.JvmLanguageAssistant")
            else -> error("Unknown language id: ${language.id}")
        }
    } catch (e: ClassNotFoundException) {
        logger.info("Language ${language.id} is disabled")
    } catch (e: IllegalStateException) {
        logger.info("Language ${language.id} is not supported")
    }
    return null
}