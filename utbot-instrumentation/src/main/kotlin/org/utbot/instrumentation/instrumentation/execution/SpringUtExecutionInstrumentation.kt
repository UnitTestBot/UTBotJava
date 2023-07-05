package org.utbot.instrumentation.instrumentation.execution

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.common.JarUtils
import org.utbot.common.hasOnClasspath
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.context.SpringInstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.phases.ExecutionPhaseFailingOnAnyException
import org.utbot.instrumentation.process.HandlerClassesLoader
import org.utbot.spring.api.SpringApi
import java.net.URL
import java.net.URLClassLoader
import java.security.ProtectionDomain

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring configuration and profiles and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    private val delegateInstrumentation: UtExecutionInstrumentation,
    private val springSettings: SpringSettings,
    private val beanDefinitions: List<BeanDefinitionData>,
    private val buildDirs: Array<URL>,
) : Instrumentation<UtConcreteExecutionResult> by delegateInstrumentation {

    private lateinit var instrumentationContext: SpringInstrumentationContext
    private lateinit var userSourcesClassLoader: URLClassLoader

    private val relatedBeansCache = mutableMapOf<Class<*>, Set<String>>()

    private val springApi: SpringApi get() = instrumentationContext.springApi

    private object SpringBeforeTestMethodPhase : ExecutionPhaseFailingOnAnyException()
    private object SpringAfterTestMethodPhase : ExecutionPhaseFailingOnAnyException()

    companion object {
        private val logger = getLogger<SpringUtExecutionInstrumentation>()
        private const val SPRING_COMMONS_JAR_FILENAME = "utbot-spring-commons-shadow.jar"
    }

    override fun init(pathsToUserClasses: Set<String>) {
        HandlerClassesLoader.addUrls(
            listOf(
                JarUtils.extractJarFileFromResources(
                    jarFileName = SPRING_COMMONS_JAR_FILENAME,
                    jarResourcePath = "lib/$SPRING_COMMONS_JAR_FILENAME",
                    targetDirectoryName = "spring-commons"
                ).path
            )
        )

        userSourcesClassLoader = URLClassLoader(buildDirs, null)
        instrumentationContext = SpringInstrumentationContext(springConfig, delegateInstrumentation.instrumentationContext)
        delegateInstrumentation.instrumentationContext = instrumentationContext
        delegateInstrumentation.init(pathsToUserClasses)
        springApi.beforeTestClass()
    }

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtConcreteExecutionResult {
        getRelevantBeans(clazz).forEach { beanName -> springApi.resetBean(beanName) }

        return delegateInstrumentation.invoke(clazz, methodSignature, arguments, parameters) { invokeBasePhases ->
            // NB! beforeTestMethod() and afterTestMethod() are intentionally called inside phases,
            //     so they are executed in one thread with method under test
            executePhaseInTimeout(SpringBeforeTestMethodPhase) { springApi.beforeTestMethod() }
            try {
                invokeBasePhases()
            } finally {
                executePhaseInTimeout(SpringAfterTestMethodPhase) { springApi.afterTestMethod() }
            }
        }
    }

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
    ): ByteArray? =
        // we do not transform Spring classes as it takes too much time

        // maybe we should still transform classes related to data validation
        // (e.g. from packages "javax/persistence" and "jakarta/persistence"),
        // since traces from such classes can be particularly useful for feedback to fuzzer
        if (userSourcesClassLoader.hasOnClasspath(className.replace("/", "."))) {
            delegateInstrumentation.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
        } else {
            null
        }
}