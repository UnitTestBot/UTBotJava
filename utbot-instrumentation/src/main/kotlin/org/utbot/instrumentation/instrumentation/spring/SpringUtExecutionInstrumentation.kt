package org.utbot.instrumentation.instrumentation.spring

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.common.JarUtils
import org.utbot.common.hasOnClasspath
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.SpringSettings.*
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import org.utbot.instrumentation.instrumentation.execution.context.InstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.phases.ExecutionPhaseFailingOnAnyException
import org.utbot.instrumentation.instrumentation.execution.phases.PhasesController
import org.utbot.spring.api.SpringApi
import java.net.URL
import java.net.URLClassLoader
import java.security.ProtectionDomain

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring configuration and profiles and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    instrumentationContext: InstrumentationContext,
    delegateInstrumentationFactory: UtExecutionInstrumentation.Factory<*>,
    springSettings: PresentSpringSettings,
    private val beanDefinitions: List<BeanDefinitionData>,
    buildDirs: Array<URL>,
) : UtExecutionInstrumentation {
    private val instrumentationContext = SpringInstrumentationContext(springSettings, instrumentationContext)
    private val delegateInstrumentation = delegateInstrumentationFactory.create(this.instrumentationContext)
    private val userSourcesClassLoader = URLClassLoader(buildDirs, null)

    private val relatedBeansCache = mutableMapOf<Class<*>, Set<String>>()

    private val springApi: SpringApi get() = instrumentationContext.springApi

    private object SpringBeforeTestMethodPhase : ExecutionPhaseFailingOnAnyException()
    private object SpringAfterTestMethodPhase : ExecutionPhaseFailingOnAnyException()

    companion object {
        private val logger = getLogger<SpringUtExecutionInstrumentation>()
        private const val SPRING_COMMONS_JAR_FILENAME = "utbot-spring-commons-shadow.jar"
    }

    fun tryLoadingSpringContext(): ConcreteContextLoadingResult {
        val apiProviderResult = instrumentationContext.springApiProviderResult
        return ConcreteContextLoadingResult(
            contextLoaded = apiProviderResult.result.isSuccess,
            exceptions = apiProviderResult.exceptions
        )
    }

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?,
        phasesWrapper: PhasesController.(invokeBasePhases: () -> UtConcreteExecutionResult) -> UtConcreteExecutionResult
    ): UtConcreteExecutionResult {
        getRelevantBeans(clazz).forEach { beanName -> springApi.resetBean(beanName) }
        return delegateInstrumentation.invoke(clazz, methodSignature, arguments, parameters) { invokeBasePhases ->
            phasesWrapper {
                // NB! beforeTestMethod() and afterTestMethod() are intentionally called inside phases,
                //     so they are executed in one thread with method under test
                // NB! beforeTestMethod() and afterTestMethod() are executed without timeout, because:
                //     - if the invokeBasePhases() times out, we still want to execute afterTestMethod()
                //     - first call to beforeTestMethod() can take significant amount of time due to class loading & transformation
                executePhaseWithoutTimeout(SpringBeforeTestMethodPhase) { springApi.beforeTestMethod() }
                try {
                    invokeBasePhases()
                } finally {
                    executePhaseWithoutTimeout(SpringAfterTestMethodPhase) { springApi.afterTestMethod() }
                }
            }
        }
    }

    override fun getStaticField(fieldId: FieldId): Result<*> = delegateInstrumentation.getStaticField(fieldId)

    private fun getRelevantBeans(clazz: Class<*>): Set<String> = relatedBeansCache.getOrPut(clazz) {
        beanDefinitions
            .filter { it.beanTypeName == clazz.name }
            // forces `getBean()` to load Spring classes,
            // otherwise execution of method under test may fail with timeout
            .onEach { springApi.getBean(it.beanName) }
            .flatMap { springApi.getDependenciesForBean(it.beanName, userSourcesClassLoader) }
            .toSet()
            .also { logger.info { "Detected relevant beans for class ${clazz.name}: $it" } }
    }

    fun getBean(beanName: String): Any = springApi.getBean(beanName)

    fun getRepositoryDescriptions(classId: ClassId): Set<SpringRepositoryId> {
        val relevantBeanNames = getRelevantBeans(classId.jClass)
        val repositoryDescriptions = springApi.resolveRepositories(relevantBeanNames.toSet(), userSourcesClassLoader)
        return repositoryDescriptions.map { repositoryDescription ->
            SpringRepositoryId(
                repositoryDescription.beanName,
                ClassId(repositoryDescription.repositoryName),
                ClassId(repositoryDescription.entityName),
            )
        }.toSet()
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? {
        // always transform `MockMvc` to avoid empty coverage when testing controllers
        val isMockMvc = className == "org/springframework/test/web/servlet/MockMvc"

        // we do not transform Spring classes as it takes too much time

        // maybe we should still transform classes related to data validation
        // (e.g. from packages "javax/persistence" and "jakarta/persistence"),
        // since traces from such classes can be particularly useful for feedback to fuzzer
        return if (isMockMvc || userSourcesClassLoader.hasOnClasspath(className.replace("/", "."))) {
            delegateInstrumentation.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
        } else {
            null
        }
    }

    class Factory(
        private val delegateInstrumentationFactory: UtExecutionInstrumentation.Factory<*>,
        private val springSettings: PresentSpringSettings,
        private val beanDefinitions: List<BeanDefinitionData>,
        private val buildDirs: Array<URL>,
    ) : UtExecutionInstrumentation.Factory<SpringUtExecutionInstrumentation> {
        override val additionalRuntimeClasspath: Set<String>
            get() = super.additionalRuntimeClasspath + JarUtils.extractJarFileFromResources(
                jarFileName = SPRING_COMMONS_JAR_FILENAME,
                jarResourcePath = "lib/$SPRING_COMMONS_JAR_FILENAME",
                targetDirectoryName = "spring-commons"
            ).path

        override val forceDisableSandbox: Boolean
            get() = true

        override fun create(instrumentationContext: InstrumentationContext): SpringUtExecutionInstrumentation =
            SpringUtExecutionInstrumentation(
                instrumentationContext,
                delegateInstrumentationFactory,
                springSettings,
                beanDefinitions,
                buildDirs
            )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Factory

            if (delegateInstrumentationFactory != other.delegateInstrumentationFactory) return false
            if (springSettings != other.springSettings) return false
            if (beanDefinitions != other.beanDefinitions) return false
            return buildDirs.contentEquals(other.buildDirs)
        }

        override fun hashCode(): Int {
            var result = delegateInstrumentationFactory.hashCode()
            result = 31 * result + springSettings.hashCode()
            result = 31 * result + beanDefinitions.hashCode()
            result = 31 * result + buildDirs.contentHashCode()
            return result
        }
    }
}