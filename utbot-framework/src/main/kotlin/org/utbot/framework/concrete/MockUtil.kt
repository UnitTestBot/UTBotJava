package org.utbot.framework.concrete

import org.mockito.stubbing.Answer
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.util.executableId

/**
 * Create [Answer] that mocks each method by specified values in order.
 * If there isn't next value it will call real method.
 */
fun buildAnswer(
    methodToValues: Map<out ExecutableId, List<Any?>>
) : Answer<Any?> {
    val pointers = methodToValues.mapValues { (_, _) -> 0 }.toMutableMap()
    return Answer { invocation ->
        with(invocation.method) {
            pointers[executableId].let { pointer ->
                methodToValues[executableId].let { values ->
                    if (pointer != null && values != null && pointer < values.size) {
                        pointers[executableId] = pointer + 1
                        values[pointer].run {
                            if (this is Unit) null else this
                        }
                    } else {
                        invocation.callRealMethod()
                    }
                }
            }
        }
    }
}
