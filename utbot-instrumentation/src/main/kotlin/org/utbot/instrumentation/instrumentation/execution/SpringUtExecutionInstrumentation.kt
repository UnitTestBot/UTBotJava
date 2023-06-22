package org.utbot.instrumentation.instrumentation.execution

import org.utbot.common.JarUtils
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.mock.SpringInstrumentationContext
import org.utbot.instrumentation.process.HandlerClassesLoader
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.repositoryWrapper.RepositoryInteraction
import java.security.ProtectionDomain
import java.util.IdentityHashMap

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring config and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    private val delegateInstrumentation: UtExecutionInstrumentation,
    private val springConfig: String,
    private val beanDefinitions: List<BeanDefinitionData>,
) : Instrumentation<UtConcreteExecutionResult> by delegateInstrumentation {
    private lateinit var instrumentationContext: SpringInstrumentationContext

    private val relatedBeansCache = mutableMapOf<Class<*>, Set<String>>()

    private val springContext: ContextWrapper get() = instrumentationContext.springContext

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

        instrumentationContext = SpringInstrumentationContext(springConfig)
        delegateInstrumentation.instrumentationContext = instrumentationContext
        delegateInstrumentation.init(pathsToUserClasses)
    }

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtConcreteExecutionResult {
        RepositoryInteraction.recordedInteractions.clear()

        val beanNamesToReset: Set<String> = getRelevantBeanNames(clazz)
        val repositoryDefinitions = springContext.resolveRepositories(beanNamesToReset)

        beanNamesToReset.forEach { beanName -> springContext.resetBean(beanName) }
        val jdbcTemplate = getBean("jdbcTemplate")

        for (repositoryDefinition in repositoryDefinitions) {
            val truncateTableCommand = "TRUNCATE TABLE ${repositoryDefinition.tableName}"
            jdbcTemplate::class.java
                .getMethod("execute", truncateTableCommand::class.java)
                .invoke(jdbcTemplate, truncateTableCommand)

            val restartIdCommand = "ALTER TABLE ${repositoryDefinition.tableName} ALTER COLUMN id RESTART WITH 1"
            jdbcTemplate::class.java
                .getMethod("execute", restartIdCommand::class.java)
                .invoke(jdbcTemplate, restartIdCommand)
        }

        return delegateInstrumentation.invoke(clazz, methodSignature, arguments, parameters)
    }

    private fun getRelevantBeanNames(clazz: Class<*>): Set<String> = relatedBeansCache.getOrPut(clazz) {
        beanDefinitions
            .filter { it.beanTypeFqn == clazz.name }
            .flatMap { springContext.getDependenciesForBean(it.beanName) }
            .toSet()
            .also { logger.info { "Detected relevant beans for class ${clazz.name}: $it" } }
    }

    fun getBean(beanName: String): Any = springContext.getBean(beanName)

    fun getRepositoryDescriptions(classId: ClassId): Set<SpringRepositoryId> {
        val relevantBeanNames = getRelevantBeanNames(classId.jClass)
        val repositoryDescriptions = springContext.resolveRepositories(relevantBeanNames.toSet())
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
        // TODO: automatically detect which libraries we don't want to transform (by total transformation time)
        if (listOf(
                "org/springframework",
                "com/fasterxml",
                "org/hibernate",
                "org/apache",
                "org/h2",
                "javax/",
                "ch/qos",
            ).any { className.startsWith(it) }
        ) {
            null
        } else {
            delegateInstrumentation.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
        }
}