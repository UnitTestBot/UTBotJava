package org.utbot.fuzzing.spring.unit

import com.google.common.reflect.TypeResolver
import mu.KotlinLogging
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Scope
import org.utbot.fuzzing.ScopeProperty
import org.utbot.fuzzing.Seed
import org.utbot.fuzzer.jType
import org.utbot.fuzzer.toTypeParametrizedByTypeVariables
import org.utbot.fuzzer.typeToken
import org.utbot.fuzzing.toFuzzerType

val methodsToMockProperty = ScopeProperty<Set<MethodId>>(
    description = "Method ids that can be mocked by `MockValueProvider`"
)

// TODO shouldn't be used for primitives and other "easy" to create types
class MockValueProvider(private val idGenerator: IdGenerator<Int>) : JavaValueProvider {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val loggedMockedMethods = mutableSetOf<MethodId>()
        private val loggedUnresolvedMethods = mutableSetOf<MethodId>()
    }

    private val methodsToMock = mutableSetOf<MethodId>()

    override fun enrich(description: FuzzedDescription, type: FuzzedType, scope: Scope) {
        val publicMethods = type.classId.jClass.methods.map { it.executableId }
        publicMethods.intersect(methodsToMock).takeIf { it.isNotEmpty() }?.let {
            scope.putProperty(methodsToMockProperty, it)
        }
    }

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> =
        sequenceOf(Seed.Recursive(
            construct = Routine.Create(types = emptyList()) { emptyMockFuzzedValue(type.classId) },
            empty = Routine.Empty { emptyMockFuzzedValue(type.classId) },
            modify = (description.scope?.getProperty(methodsToMockProperty)?.asSequence() ?: emptySequence()).map { methodId ->
                val methodDeclaringClass = methodId.classId.jClass

                val returnType = try {
                    TypeResolver().where(
                        methodDeclaringClass.toTypeParametrizedByTypeVariables(),
                        @Suppress("UNCHECKED_CAST")
                        type.jType.typeToken.getSupertype(methodDeclaringClass as Class<in Any>).type
                    ).resolveType(methodId.method.genericReturnType)
                } catch (e: Exception) {
                    if (loggedUnresolvedMethods.add(methodId))
                        logger.error(e) { "Failed to resolve return type for $methodId, using unresolved generic type" }

                    methodId.method.genericReturnType
                }

                // TODO accept `List<resolvedReturnType>` instead of singular `resolvedReturnType`
                Routine.Call(types = listOf(toFuzzerType(returnType, description.typeCache))) { instance, (value) ->
                    if (loggedMockedMethods.add(methodId))
                        logger.info { "Actually mocked $methodId for the first time" }

                    (instance.model as UtCompositeModel).mocks[methodId] = listOf(value.model)
                }
            }
        ))

    private fun emptyMockFuzzedValue(classId: ClassId) = UtCompositeModel(
        id = idGenerator.createId(),
        classId = classId,
        isMock = true,
        canHaveRedundantOrMissingMocks = true,
    ).fuzzed { summary = "%var% = mock()" }

    fun addMockingCandidates(detectedMockingCandidates: Set<MethodId>) =
        detectedMockingCandidates.forEach { methodId ->
            if (methodsToMock.add(methodId))
                logger.info { "Detected that $methodId may need mocking" }
        }
}