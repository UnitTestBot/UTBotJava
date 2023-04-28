package org.utbot.instrumentation.instrumentation.execution.mock

class SpringInstrumentationContext(private val beanGetter: (beanName: String) -> Any) : InstrumentationContext() {
    fun getBean(beanName: String): Any = beanGetter(beanName)
}