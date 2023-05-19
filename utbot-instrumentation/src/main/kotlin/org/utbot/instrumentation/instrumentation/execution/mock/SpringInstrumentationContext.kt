package org.utbot.instrumentation.instrumentation.execution.mock

abstract class SpringInstrumentationContext : InstrumentationContext() {
    abstract fun getBean(beanName: String): Any
    abstract fun saveToRepository(repository: Any, entity: Any)
}