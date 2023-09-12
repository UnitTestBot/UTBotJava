package org.utbot.fuzzing.spring

import mu.KotlinLogging
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.providers.nullFuzzedValue

/**
 * Returns [JavaValueProvider] that only uses `null` value for classes that mention (in their
 * constructor, method, or field signatures) classes that are not present on the classpath.
 */
fun JavaValueProvider.useNullForPartiallyUnresolvableClasses() =
    PartiallyUnresolvableClassValueProvider().withFallback(this)

private class PartiallyUnresolvableClassValueProvider : JavaValueProvider {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val classResolvabilityCache = mutableMapOf<ClassId, Boolean>()

    override fun accept(type: FuzzedType): Boolean = !classResolvabilityCache.getOrPut(type.classId) {
        runCatching {
            type.classId.allConstructors.toList()
            type.classId.allMethods.toList()
            type.classId.allDeclaredFieldIds.toList()
        }.onFailure { e ->
            logger.warn { "Failed to resolve ${type.classId} dependencies, using `null` value, cause: $e" }
        }.isSuccess
    }

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> =
        sequenceOf(Seed.Simple(nullFuzzedValue(type.classId)))
}