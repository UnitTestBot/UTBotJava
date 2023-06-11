package org.utbot.instrumentation.instrumentation.execution

import org.utbot.common.JarUtils
import com.jetbrains.rd.util.getLogger
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.mock.SpringInstrumentationContext
import org.utbot.instrumentation.process.HandlerClassesLoader
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.repositoryWrapper.RepositoryInteraction
import java.security.ProtectionDomain

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring config and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    private val delegateInstrumentation: UtExecutionInstrumentation,
    private val springConfig: String
) : Instrumentation<UtConcreteExecutionResult> by delegateInstrumentation {
    private lateinit var instrumentationContext: SpringInstrumentationContext
    private val springContext: ContextWrapper get() = instrumentationContext.springContext

    companion object {
        private val logger = getLogger<SpringUtExecutionInstrumentation>()
        private const val SPRING_COMMONS_JAR_FILENAME = "utbot-spring-commons-shadow.jar"
    }

    override fun init(pathsToUserClasses: Set<String>) {
        HandlerClassesLoader.addUrls(listOf(JarUtils.extractJarFileFromResources(
            jarFileName = SPRING_COMMONS_JAR_FILENAME,
            jarResourcePath = "lib/$SPRING_COMMONS_JAR_FILENAME",
            targetDirectoryName = "spring-commons"
        ).path))
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
        // TODO properly detect which beans need to be reset, right now "orderRepository" and "orderService" are hardcoded
        val beanNamesToReset = listOf("orderRepository", "orderService")

        beanNamesToReset.forEach { beanNameToReset ->
            val beanDefToReset = springContext.getBeanDefinition(beanNameToReset)
            springContext.removeBeanDefinition(beanNameToReset)
            springContext.registerBeanDefinition(beanNameToReset, beanDefToReset)
        }

        val jdbcTemplate = getBean("jdbcTemplate")
        // TODO properly detect which repositories need to be cleared, right now "orders" is hardcoded
        val sql = "TRUNCATE TABLE orders"
        jdbcTemplate::class.java
            .getMethod("execute", sql::class.java)
            .invoke(jdbcTemplate, sql)
        val sql2 = "ALTER TABLE orders ALTER COLUMN id RESTART WITH 1"
        jdbcTemplate::class.java
            .getMethod("execute", sql::class.java)
            .invoke(jdbcTemplate, sql2)

        return delegateInstrumentation.invoke(clazz, methodSignature, arguments, parameters)
    }

    fun getBean(beanName: String): Any = springContext.getBean(beanName)

    fun saveToRepository(repository: Any, entity: Any) {
        // ignore repository interactions done during repository fill up
        val savedRecordedRepositoryResponses = RepositoryInteraction.recordedInteractions.toList()
        repository::class.java
            .getMethod("save", Any::class.java)
            .invoke(repository, entity)
        RepositoryInteraction.recordedInteractions.clear()
        RepositoryInteraction.recordedInteractions.addAll(savedRecordedRepositoryResponses)
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? =
        // TODO automatically detect which libraries we don't want to transform (by total transformation time)
        // transforming Spring takes too long
        if (listOf(
                "org/springframework",
                "com/fasterxml",
                "org/hibernate",
                "org/apache",
                "org/h2"
            ).any { className.startsWith(it) }
        ) {
            null
        } else {
            delegateInstrumentation.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
        }
}