package org.utbot.instrumentation.instrumentation.execution

import org.springframework.context.ConfigurableApplicationContext
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.mock.SpringInstrumentationContext
import org.utbot.spring.context.InstantiationContext
import org.utbot.spring.instantiator.SpringApplicationInstantiatorFacade
import kotlin.random.Random

/**
 * UtExecutionInstrumentation wrapper that is aware of Spring config and initialises Spring context
 */
class SpringUtExecutionInstrumentation(
    private val instrumentation: UtExecutionInstrumentation,
    private val springConfig: String
) : Instrumentation<UtConcreteExecutionResult> by instrumentation {
    private lateinit var springContext: ConfigurableApplicationContext

    override fun init(pathsToUserClasses: Set<String>) {
        instrumentation.instrumentationContext = SpringInstrumentationContext(this::getBean)

        instrumentation.init(pathsToUserClasses)

        val classLoader = UtContext.currentContext()!!.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        val configClass = classLoader.loadClass(springConfig)
        // TODO recreate context/app every time with change method under test
        // TODO obtain profile expression here
        springContext = SpringApplicationInstantiatorFacade(
            InstantiationContext(arrayOf(configClass), profileExpression = null)
        )
            .instantiate()
            ?: error("Cannot instantiate Spring context")
    }

    fun getBean(beanName: String): Any = springContext.getBean(beanName)
}