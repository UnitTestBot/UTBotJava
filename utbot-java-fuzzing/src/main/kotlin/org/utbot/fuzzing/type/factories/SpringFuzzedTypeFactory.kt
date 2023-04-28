package org.utbot.fuzzing.type.factories

import org.utbot.framework.plugin.api.ClassId
import org.utbot.fuzzer.AutowiredFuzzedType
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzing.providers.AutowiredValueProvider
import java.lang.reflect.Type

class SpringFuzzedTypeFactory(
    private val delegate: FuzzedTypeFactory = SimpleFuzzedTypeFactory(),
    private val autowiredValueProvider: AutowiredValueProvider,
    private val beanNamesFinder: (ClassId) -> List<String>
) : FuzzedTypeFactory {
    override fun createFuzzedType(type: Type, isThisInstance: Boolean): FuzzedType {
        val fuzzedType = delegate.createFuzzedType(type, isThisInstance)
        if (!isThisInstance) return fuzzedType
        val beanNames = beanNamesFinder(fuzzedType.classId)
        return if (beanNames.isEmpty()) fuzzedType else
            AutowiredFuzzedType(fuzzedType, beanNames, autowiredValueProvider)
    }
}