package org.utbot.instrumentation.instrumentation.execution

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.common.JarUtils
import org.utbot.common.hasOnClasspath
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.context.SpringInstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.phases.ExecutionPhaseFailingOnAnyException
import org.utbot.instrumentation.process.HandlerClassesLoader
import org.utbot.spring.api.context.ContextWrapper
import java.net.URL
import java.net.URLClassLoader
import java.security.ProtectionDomain

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring config and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    private val delegateInstrumentation: UtExecutionInstrumentation,
    private val springConfig: String,
    private val beanDefinitions: List<BeanDefinitionData>,
    private val buildDirs: Array<URL>,
) : Instrumentation<UtConcreteExecutionResult> by delegateInstrumentation {

    private lateinit var instrumentationContext: SpringInstrumentationContext
    private lateinit var userSourcesClassLoader: URLClassLoader

    private val relatedBeansCache = mutableMapOf<Class<*>, Set<String>>()

    private val springContext: ContextWrapper get() = instrumentationContext.springContext

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
        springContext.beforeTestClass()
    }

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtConcreteExecutionResult {
        getRelevantBeanNames(clazz).forEach { beanName -> springContext.resetBean(beanName) }

        return delegateInstrumentation.invoke(clazz, methodSignature, arguments, parameters) { basePhases ->
            // NB! beforeTestMethod() and afterTestMethod() are intentionally called inside phases,
            //     so they are executed in one thread with method under test
            executePhaseInTimeout(SpringBeforeTestMethodPhase) { springContext.beforeTestMethod() }
            try {
                basePhases()
            } finally {
                executePhaseInTimeout(SpringAfterTestMethodPhase) { springContext.afterTestMethod() }
            }
        }
    }

    private fun getRelevantBeanNames(clazz: Class<*>): Set<String> = relatedBeansCache.getOrPut(clazz) {
        beanDefinitions
            .filter { it.beanTypeFqn == clazz.name }
            // TODO move forcing `getBean()` somewhere else (we need to once forcefully get beans under test
            //  before `executePhaseInTimeout` to force loading and transformation of Spring classes without timeout)
            .onEach { springContext.getBean(it.beanName) }
            .flatMap { springContext.getDependenciesForBean(it.beanName, userSourcesClassLoader) }
            .toSet()
            .also { logger.info { "Detected relevant beans for class ${clazz.name}: $it" } }
    }

    fun getBean(beanName: String): Any = springContext.getBean(beanName)

    fun getRepositoryDescriptions(classId: ClassId): Set<SpringRepositoryId> {
        val relevantBeanNames = getRelevantBeanNames(classId.jClass)
        val repositoryDescriptions = springContext.resolveRepositories(relevantBeanNames.toSet(), userSourcesClassLoader)
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
        // transforming Spring takes too long
        // maybe we should also transform classes in "javax/persistence" and "jakarta/persistence" to give fuzzer more feedback
        if (userSourcesClassLoader.hasOnClasspath(className.replace("/", "."))) {
            delegateInstrumentation.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
        } else {
            null
        }
}