package org.utbot.framework.concrete

import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.instrumentation.mock.MockConfig
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier


/**
 * Helper class that handles work with mock fields.
 *
 * This object is created for every mocked method for a specific [instance]. There can be several [MethodMockController]s
 * for the same [instance], but they will be constructed with different method parameters.
 *
 * @param [mockedValues] consists of return elements for this mocked method in the order of calling.
 * @param [instance] is an object with mocked methods. Should be `null` for mocking static methods.
 */
class MethodMockController(
    clazz: Class<*>,
    method: Method,
    val instance: Any?,
    mockedValues: List<Any?>,
    instrumentationContext: InstrumentationContext
) : MockController {
    private val isMockField: Field

    init {
        if (!Modifier.isStatic(method.modifiers) && instance == null) {
            error("$method is an instance method, but instance is null!")
        }

        val id = instrumentationContext.methodSignatureToId[method.signature]

        isMockField = clazz.declaredFields.firstOrNull { it.name == MockConfig.IS_MOCK_FIELD + id }
            ?: error("No field ${MockConfig.IS_MOCK_FIELD + id} in $clazz")

        isMockField.withAccessibility {
            isMockField.set(instance, true)
        }

        if (method.returnType != Void.TYPE) {
            InstrumentationContext.MockGetter.updateMocks(instance, method, mockedValues)
        }
    }

    override fun close() {
        isMockField.withAccessibility {
            isMockField.set(instance, false)
        }
    }
}