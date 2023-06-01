package org.utbot.instrumentation.instrumentation.execution

import org.utbot.common.JarUtils
import com.jetbrains.rd.util.getLogger
import org.utbot.framework.plugin.api.RepositoryInteractionModel
import org.utbot.framework.plugin.api.UtAutowiredStateAfterModel
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.idOrNull
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.mock.SpringInstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.phases.ModelConstructionPhase
import org.utbot.instrumentation.process.HandlerClassesLoader
import org.utbot.spring.api.repositoryWrapper.RepositoryInteraction
import java.security.ProtectionDomain
import kotlin.random.Random

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring config and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    private val instrumentation: UtExecutionInstrumentation,
    private val springConfig: String
) : Instrumentation<UtConcreteExecutionResult> by instrumentation {
    private lateinit var springContext: Any

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

        instrumentation.instrumentationContext = object : SpringInstrumentationContext() {
            override fun getBean(beanName: String) =
                this@SpringUtExecutionInstrumentation.getBean(beanName)

            override fun saveToRepository(repository: Any, entity: Any) =
                this@SpringUtExecutionInstrumentation.saveToRepository(repository, entity)
        }

        instrumentation.init(pathsToUserClasses)

        val classLoader = utContext.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        val primarySources = arrayOf(
            classLoader.loadClass(springConfig),
            classLoader.loadClass("org.utbot.spring.repositoryWrapper.RepositoryWrapperConfiguration")
        )

        // Setting server.port value to 0 means given Spring to select any appropriate port itself.
        // See https://stackoverflow.com/questions/21083170/how-to-configure-port-for-a-spring-boot-application
        val args = arrayOf("--server.port=0")

        // TODO if we don't have SpringBoot just create ApplicationContext here, reuse code from utbot-spring-analyzer
        // TODO recreate context/app every time whenever we change method under test
        val springAppClass =
            classLoader.loadClass("org.springframework.boot.SpringApplication")
        springContext = springAppClass
            .getMethod("run", primarySources::class.java, args::class.java)
            .invoke(null, primarySources, args)
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
            val beanDefToReset = springContext::class.java
                .getMethod("getBeanDefinition", String::class.java)
                .invoke(springContext, beanNameToReset)
            springContext::class.java
                .getMethod("removeBeanDefinition", String::class.java)
                .invoke(springContext, beanNameToReset)
            springContext::class.java
                .getMethod(
                    "registerBeanDefinition",
                    String::class.java,
                    utContext.classLoader.loadClass("org.springframework.beans.factory.config.BeanDefinition")
                )
                .invoke(springContext, beanNameToReset, beanDefToReset)
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

        return instrumentation.invoke(clazz, methodSignature, arguments, parameters) { executionResult ->
            executePhaseInTimeout(modelConstructionPhase) {
                executionResult.copy(
                    stateAfter = executionResult.stateAfter.copy(
                        thisInstance = executionResult.stateAfter.thisInstance?.let { thisInstance ->
                            UtAutowiredStateAfterModel(
                                id = thisInstance.idOrNull(),
                                classId = thisInstance.classId,
                                origin = thisInstance,
                                repositoryInteractions = constructRepositoryInteractionModels()
                            )
                        }
                    )
                )
            }
        }
    }

    fun getBean(beanName: String): Any =
        springContext::class.java
            .getMethod("getBean", String::class.java)
            .invoke(springContext, beanName)

    fun saveToRepository(repository: Any, entity: Any) {
        // ignore repository interactions done during repository fill up
        val savedRecordedRepositoryResponses = RepositoryInteraction.recordedInteractions.toList()
        repository::class.java
            .getMethod("save", Any::class.java)
            .invoke(repository, entity)
        RepositoryInteraction.recordedInteractions.clear()
        RepositoryInteraction.recordedInteractions.addAll(savedRecordedRepositoryResponses)
    }

    private fun ModelConstructionPhase.constructRepositoryInteractionModels(): List<RepositoryInteractionModel> {
        return RepositoryInteraction.recordedInteractions.map { interaction ->
            RepositoryInteractionModel(
                beanName = interaction.beanName,
                executableId = interaction.method.executableId,
                args = constructParameters(interaction.args.zip(interaction.method.parameters).map { (arg, param) ->
                    UtConcreteValue(arg, param.type)
                }),
                result = convertToExecutionResult(interaction.result, interaction.method.returnType.id)
            )
        }
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
            instrumentation.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
        }
}