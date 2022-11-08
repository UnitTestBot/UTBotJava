package org.utbot.intellij.plugin.util

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.ConfigurationWithCommandLineShortener
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManagerEx
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
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
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.childrenOfType
import java.util.Comparator
import mu.KotlinLogging
import org.utbot.intellij.plugin.models.GenerateTestsModel
import org.utbot.intellij.plugin.util.IntelliJApiHelper.run

class RunConfigurationHelper {
    class MyMapDataContext() : DataContext, UserDataHolder {
        private val myMap: MutableMap<String, Any?> = HashMap()
        private val holder = UserDataHolderBase()
        override fun getData(dataId: String): Any? {
            return myMap[dataId]
        }

        private fun put(dataId: String, data: Any?) {
            myMap[dataId] = data
        }

        fun <T> put(dataKey: DataKey<T>, data: T) {
            put(dataKey.name, data)
        }

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

        private fun RunConfiguration.isPatternBased() = this is JavaTestConfigurationBase && "pattern".contentEquals(testType, true)

        // In case we do "generate and run" for many files at once,
        // desired run configuration has to be one of "pattern" typed test configuration that may run many tests at once.
        // Thus, we sort list of all provided configurations to get desired configuration the first.
        private val rcComparator = Comparator<ConfigurationFromContext> { o1, o2 ->
            val p1 = o1.configuration.isPatternBased()
            val p2 = o2.configuration.isPatternBased()
            if (p1 xor p2) {
                return@Comparator if (p1) -1 else 1
            }
            ConfigurationFromContext.COMPARATOR.compare(o1, o2)
        }

        fun runTestsWithCoverage(
            model: GenerateTestsModel,
            testFilesPointers: MutableList<SmartPsiElementPointer<PsiFile>>,
        ) {
            PsiDocumentManager.getInstance(model.project).commitAndRunReadAction() {
                val testClasses = testFilesPointers.map { smartPointer: SmartPsiElementPointer<PsiFile> -> smartPointer.containingFile?.childrenOfType<PsiClass>()?.firstOrNull() }.filterNotNull()
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
                    run(IntelliJApiHelper.Target.THREAD_POOL, indicator = null, "Get run configurations from all producers") {
                        val configurations = ApplicationManager.getApplication().runReadAction(Computable {
                            return@Computable RunConfigurationProducer.getProducers(model.project)
                                .mapNotNull { it.findOrCreateConfigurationFromContext(myConfigurationContext) }
                                .toMutableList().sortedWith(rcComparator)
                        })

                        val settings = if (configurations.isEmpty()) null else configurations[0].configurationSettings
                        if (settings != null) {
                            val executor = if (ProgramRunner.getRunner(CoverageExecutor.EXECUTOR_ID, settings.configuration) != null) {
                                ExecutorRegistry.getInstance().getExecutorById(CoverageExecutor.EXECUTOR_ID) ?: DefaultRunExecutor.getRunExecutorInstance()
                            } else {
                                //Fallback in case 'Code Coverage for Java' plugin is not enabled
                                DefaultRunExecutor.getRunExecutorInstance()
                            }
                            run(IntelliJApiHelper.Target.EDT_LATER, null, "Start run configuration with coverage") {
                                val configuration = settings.configuration
                                if (configuration is ConfigurationWithCommandLineShortener) {
                                    configuration.shortenCommandLine = ShortenCommandLine.MANIFEST
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
}