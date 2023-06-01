package org.utbot.fuzzing.type.factories

import org.utbot.fuzzer.FuzzedType
import java.lang.reflect.Type

interface FuzzedTypeFactory {
    fun createFuzzedType(type: Type, isThisInstance: Boolean): FuzzedType
}