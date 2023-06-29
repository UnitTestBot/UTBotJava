package org.utbot.spring.context

import org.springframework.test.context.TestContextManager
import org.utbot.spring.api.instantiator.InstantiationSettings
import java.lang.reflect.Method

class SpringTestContextManagerAPI {
    fun <T> runAsTestMethod(block: () -> T): T {
        beforeTestClass()
        beforeTestMethod()
        beforeTestExecution()
        return block()
            .also {
                afterTestExecution()
                afterTestMethod()
                afterTestClass()
            }
    }

    private val dummyClass: Class<DummyClass> = DummyClass().javaClass
    private val dummyMethod: Method = dummyClass.javaClass.methods.first()
    private val testContextManager: TestContextManager = TestContextManager(dummyClass)

    private fun beforeTestClass() {
        testContextManager.beforeTestClass()
    }

    private fun afterTestClass() {
        testContextManager.afterTestClass()
    }

    private fun beforeTestMethod() {
        testContextManager.beforeTestMethod(dummyClass, dummyMethod)
    }

    private fun afterTestMethod() {
        testContextManager.afterTestMethod(dummyClass, dummyMethod, null)
    }

    private fun beforeTestExecution() {
        testContextManager.beforeTestExecution(dummyClass, dummyMethod)
    }

    private fun afterTestExecution() {
        testContextManager.afterTestExecution(dummyClass, dummyMethod, null)
    }

    companion object {
        fun create(instantiationSettings: InstantiationSettings): SpringTestContextManagerAPI {
            TODO()
        }
    }
}