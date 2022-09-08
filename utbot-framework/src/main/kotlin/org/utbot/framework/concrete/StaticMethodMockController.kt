package org.utbot.framework.concrete

import org.mockito.MockedStatic
import org.mockito.Mockito
import org.utbot.framework.plugin.api.ExecutableId


/**
 * Helper class that handles work with mockito.
 *
 * This object is created for every mocked static method for a specific [clazz]. There can't be several [StaticMethodMockController]s
 * for the same [clazz].
 *
 * @param [methodToValues] consists of return elements for mocked methods in the order of calling.
 */
class StaticMethodMockController(
    private val clazz: Class<*>,
    private val methodToValues: Map<out ExecutableId, List<Any?>>
) : MockController {

    override fun init() {
        mockStatic = Mockito.mockStatic(clazz, buildAnswer(methodToValues))
    }

    private lateinit var mockStatic: MockedStatic<*>

    override fun close() {
        mockStatic.close()
    }
}