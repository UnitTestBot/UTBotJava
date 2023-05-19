package org.utbot.instrumentation.instrumentation.execution

import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.mock.SpringInstrumentationContext
import kotlin.random.Random

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring config and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    private val instrumentation: UtExecutionInstrumentation,
    private val springConfig: String
) : Instrumentation<UtConcreteExecutionResult> by instrumentation {
    private lateinit var springContext: Any

    override fun init(pathsToUserClasses: Set<String>) {
        instrumentation.instrumentationContext = object : SpringInstrumentationContext() {
            override fun getBean(beanName: String) =
                this@SpringUtExecutionInstrumentation.getBean(beanName)

            override fun saveToRepository(repository: Any, entity: Any) =
                this@SpringUtExecutionInstrumentation.saveToRepository(repository, entity)
        }

        instrumentation.init(pathsToUserClasses)

        val classLoader = utContext.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        // TODO correctly handle the case when springConfig is an XML config
        val configClass = classLoader.loadClass(springConfig)
        val args = arrayOf("--server.port=${Random.nextInt(2048, 65536)}")
        // TODO if we don't have SpringBoot just create ApplicationContext here, reuse code from utbot-spring-analyzer
        // TODO recreate context/app every time whenever we change method under test
        val springAppClass =
            classLoader.loadClass("org.springframework.boot.SpringApplication")
        springContext = springAppClass
            .getMethod("run", configClass::class.java, args::class.java)
            .invoke(null, configClass, args)
    }

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtConcreteExecutionResult {
        val jdbcTemplate = getBean("jdbcTemplate")
        // TODO properly detect which repositories need to be cleared, right now "orders" is hardcoded
        val sql = "TRUNCATE TABLE orders"
        jdbcTemplate::class.java
            .getMethod("execute", sql::class.java)
            .invoke(jdbcTemplate, sql)
        return instrumentation.invoke(clazz, methodSignature, arguments, parameters)
    }

    fun getBean(beanName: String): Any =
        springContext::class.java
            .getMethod("getBean", String::class.java)
            .invoke(springContext, beanName)

    fun saveToRepository(repository: Any, entity: Any) {
        repository::class.java
            .getMethod("save", Any::class.java)
            .invoke(repository, entity)
    }
}