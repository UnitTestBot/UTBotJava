package org.utbot.intellij.plugin.util

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManagerEx
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import com.intellij.testFramework.MapDataContext
import mu.KotlinLogging
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.util.IntelliJApiHelper.run

class RunConfigurationHelper {
    private class MyMapDataContext : MapDataContext(), UserDataHolder {
        val holder = UserDataHolderBase()
        override fun <T : Any?> getUserData(key: Key<T>): T? {
            return holder.getUserData(key)
        }

        override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
            holder.putUserData(key, value)
        }
    }

    private class MyConfigurationContext(val context: DataContext, psiElement: PsiElement) : ConfigurationContext(psiElement) {
        override fun getDataContext() = context
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun runTestsWithCoverage(
            model: GenerateTestsModel,
            testFiles: MutableList<PsiFile>,
        ) {
            PsiDocumentManager.getInstance(model.project).commitAndRunReadAction() {
                val testClasses = testFiles.map { file: PsiFile -> file.childrenOfType<PsiClass>().firstOrNull() }.filterNotNull()
                if (testClasses.isNotEmpty()) {
                    val locations =
                        testClasses.map { PsiLocation(model.project, model.testModule, it) }.toTypedArray()
                    val mapDataContext = MyMapDataContext().also {
                        it.put(LangDataKeys.PSI_ELEMENT_ARRAY, testClasses.toTypedArray())
                        it.put(LangDataKeys.MODULE, model.testModule)
                        it.put(Location.DATA_KEYS, locations)
                        it.put(Location.DATA_KEY, locations[0])
                    }
                    val myConfigurationContext = try {
                        MyConfigurationContext(mapDataContext, testClasses[0])
                    } catch (e: Exception) {
                        logger.error { e }
                        return@commitAndRunReadAction
                    }
                    mapDataContext.putUserData(
                        ConfigurationContext.SHARED_CONTEXT,
                        myConfigurationContext
                    )
                    run(IntelliJApiHelper.Target.THREAD_POOL) {
                        val configurations = ApplicationManager.getApplication().runReadAction(Computable {
                            myConfigurationContext.configurationsFromContext
                        })

                        val settings = if (configurations.isNullOrEmpty()) null else configurations[0].configurationSettings
                        if (settings != null) {
                            val executor = if (ProgramRunner.getRunner(CoverageExecutor.EXECUTOR_ID, settings.configuration) != null) {
                                ExecutorRegistry.getInstance().getExecutorById(CoverageExecutor.EXECUTOR_ID) ?: DefaultRunExecutor.getRunExecutorInstance()
                            } else {
                                //Fallback in case 'Code Coverage for Java' plugin is not enabled
                                DefaultRunExecutor.getRunExecutorInstance()
                            }
                            ExecutionUtil.runConfiguration(settings, executor)
                            with(RunManagerEx.getInstanceEx(model.project)) {
                                if (findSettings(settings.configuration) == null) {
                                    settings.isTemporary = true
                                    addConfiguration(settings)
                                }
                                //TODO check shouldSetRunConfigurationFromContext in API 2021.3+
                                selectedConfiguration = settings
                            }
                        }
                    }
                }
            }
        }

    }
}