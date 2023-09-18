package org.utbot.intellij.plugin.language.agnostic

import mu.KotlinLogging
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

private val logger = KotlinLogging.logger {}

abstract class LanguageAssistant {

    abstract fun update(e: AnActionEvent)
    abstract fun actionPerformed(e: AnActionEvent)

    companion object {
        private val languages = mutableMapOf<String, LanguageAssistant>()

        fun get(e: AnActionEvent): LanguageAssistant? {
            val project = e.project ?: return null
            val editor = e.getData(CommonDataKeys.EDITOR)
            if (editor != null) {
                //The action is being called from editor
                e.getData(CommonDataKeys.PSI_FILE)?.language?.let { language ->
                    updateLanguages(language)
                    return languages[language.id]
                } ?: return null
            } else {
                // The action is being called from 'Project' tool window
                val language = when (val element = e.getData(CommonDataKeys.PSI_ELEMENT)) {
                    is PsiFileSystemItem -> {
                        e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.let {
                            findLanguageRecursively(project, it)
                        }
                    }
                    is PsiElement -> {
                        element.containingFile?.let { getLanguageFromFile(it) }
                    }
                    else -> {
                        val someSelection = e.getData(PlatformDataKeys.PSI_ELEMENT_ARRAY)?: return null
                        someSelection.firstNotNullOfOrNull {
                            when(it) {
                                is PsiFileSystemItem -> findLanguageRecursively(project, arrayOf(it.virtualFile))
                                is PsiElement -> it.language
                                else -> { null }
                            }
                        }
                    }
                } ?: return null

                updateLanguages(language)
                return languages[language.id]
            }
        }

        private fun updateLanguages(language: Language) {
            if (!languages.containsKey(language.id)) {
                loadWithException(language)?.let {
                    languages.put(language.id, it.kotlin.objectInstance as LanguageAssistant)
                }
            }
        }

        private fun getLanguageFromFile(file: PsiFile): Language? {
            updateLanguages(file.language)
            return if (languages.containsKey(file.language.id))
                file.language
            else
                null
        }

        private fun findLanguageRecursively(directory: PsiDirectory): Language? {
            return directory.files
                .firstNotNullOfOrNull { getLanguageFromFile(it) } ?:
                directory.subdirectories.firstNotNullOfOrNull { findLanguageRecursively(it) }
        }

        private fun findLanguageRecursively(project: Project, virtualFiles: Array<VirtualFile>): Language? {
            val psiFiles = virtualFiles.mapNotNull {
                PsiManager.getInstance(project).findFile(it)
            }
            val psiDirectories = virtualFiles.mapNotNull {
                PsiManager.getInstance(project).findDirectory(it)
            }

            val fileLanguage = psiFiles.firstNotNullOfOrNull { getLanguageFromFile(it) }
            return fileLanguage ?: psiDirectories.firstNotNullOfOrNull { findLanguageRecursively(it) }
        }
    }
}

private fun loadWithException(language: Language): Class<*>? {
    try {
        return when (language.id) {
            "Python" -> Class.forName("org.utbot.intellij.plugin.python.language.PythonLanguageAssistant")
            "ECMAScript 6" -> Class.forName("org.utbot.intellij.plugin.js.language.JsLanguageAssistant")
            "go" -> Class.forName("org.utbot.intellij.plugin.go.language.GoLanguageAssistant")
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