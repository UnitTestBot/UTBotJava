package org.utbot.instrumentation.instrumentation.execution

import org.utbot.framework.plugin.api.util.UtContext
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
        instrumentation.instrumentationContext = SpringInstrumentationContext(this::getBean)

        instrumentation.init(pathsToUserClasses)

        val classLoader = UtContext.currentContext()!!.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        val configClass = classLoader.loadClass(springConfig)
        val args = arrayOf("--server.port=${Random.nextInt(2048, 65536)}")
        // TODO if we don't have SpringBoot just create ApplicationContext here, reuse code from utbot-spring-analyzer
        // TODO recreate context/app every time with change method under test
        val springAppClass =
            classLoader.loadClass("org.springframework.boot.SpringApplication")
        springContext = springAppClass
            .getMethod("run", configClass::class.java, args::class.java)
            .invoke(null, configClass, args)
    }

    // TODO use `springContext.getBean(String)` here
    //  should be fixed together with ("use bean names here, which should be made available here via SpringApplicationContext" in UtBotSymbolicEngine)
    fun getBean(beanName: String): Any =
        getBean(UtContext.currentContext()!!.classLoader.loadClass(beanName))

    private fun getBean(clazz: Class<*>): Any =
        springContext::class.java
            .getMethod("getBean", clazz::class.java)
            .invoke(springContext, clazz)
}