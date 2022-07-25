package org.utbot.framework.concrete

import java.lang.reflect.Method
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

/**
 * Helper class for working with [Mockito] using given classLoader.
 */
class MockitoProvider(classLoader: ClassLoader) {
    private val mockitoClass = classLoader.loadClass(Mockito::class.java.name)
    private val answerClass = classLoader.loadClass(Answer::class.java.name)
    private val mockFunction = mockitoClass.getMethod("mock", Class::class.java, answerClass)
    private val answerConstructor = classLoader.loadClass(AnswerByValues::class.java.name).constructors.first()

    fun mock(clazz: Class<*>, answer: Any): Any {
        require(answerClass.isInstance(answer))
        return mockFunction.invoke(null, clazz, answer)
    }

    fun buildAnswer(
        concreteValues: Map<Any?, List<Any?>>,
        pointers: MutableMap<Any?, Int>,
        keyGet: java.util.function.Function<Method, Any?>
    ): Any {
        return answerConstructor.newInstance(concreteValues, pointers, keyGet)
    }

}

class AnswerByValues(
    private val concreteValues: Map<Any?, List<Any?>>,
    private val pointers: MutableMap<Any?, Int>,
    private val keyGet: java.util.function.Function<Method, Any?>
) : Answer<Any?> {
    override fun answer(invocation: InvocationOnMock): Any? =
        with(invocation.method) {
            val key = keyGet.apply(this)
            pointers[key].let { pointer ->
                concreteValues[key].let { values ->
                    if (pointer != null && values != null && pointer < values.size) {
                        pointers[key] = pointer + 1
                        values[pointer]
                    } else {
                        invocation.callRealMethod()
                    }
                }
            }
        }
}
