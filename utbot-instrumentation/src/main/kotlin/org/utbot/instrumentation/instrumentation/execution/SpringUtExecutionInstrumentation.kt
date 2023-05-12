package org.utbot.instrumentation.instrumentation.execution

import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.mock.SpringInstrumentationContext

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
        val configClass = classLoader.loadClass(springConfig)
        // TODO use BootstrapContext (like in SpringBootTestContextBootstrapper) to create TestContext and get applicationContext out of it
        //  but before that consider improving build.gradle (in utbot-instrumentation) so we don't have to use reflection for Spring
        val springContextClass =
            classLoader.loadClass("org.springframework.context.annotation.AnnotationConfigApplicationContext")
        val sources = arrayOf(configClass)
        springContext = springContextClass.getConstructor(sources::class.java).newInstance(sources)
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