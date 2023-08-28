package org.utbot.framework.context.custom

import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.providers.AnyDepthNullValueProvider
import org.utbot.fuzzing.providers.MapValueProvider
import org.utbot.fuzzing.spring.unit.MockValueProvider
import org.utbot.fuzzing.providers.NullValueProvider
import org.utbot.fuzzing.providers.ObjectValueProvider
import org.utbot.fuzzing.providers.StringValueProvider
import org.utbot.fuzzing.providers.anyObjectValueProvider
import org.utbot.fuzzing.spring.decorators.filterTypes
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

/**
 * Allows fuzzer to use mocks in accordance with [JavaFuzzingContext.mockStrategy].
 *
 * NOTE:
 *   - fuzzer won't mock types, that have *specific* value providers (e.g. [MapValueProvider] and [StringValueProvider])
 *   - [ObjectValueProvider] and [NullValueProvider] do not count as *specific* value providers
 */
fun JavaFuzzingContext.allowMocks() =
    MockingJavaFuzzingContext(delegateContext = this)

class MockingJavaFuzzingContext(
    val delegateContext: JavaFuzzingContext,
) : JavaFuzzingContext by delegateContext {
    private val mockValueProvider = MockValueProvider(delegateContext.idGenerator)

    override val valueProvider: JavaValueProvider =
        // NOTE: we first remove `NullValueProvider` and `ObjectValueProvider` from `delegateContext.valueProvider`
        //       and then add them back as a part of our `withFallback` so they have the same priority as
        //       `mockValueProvider`, otherwise mocks will never be used where `null` or new object can be used.
        delegateContext.valueProvider
            .except { it is NullValueProvider }
            .except { it is ObjectValueProvider }
            .withFallback(
                mockValueProvider
                    .filterTypes { type ->
                        mockStrategy.eligibleToMock(
                            classToMock = type.classId,
                            classUnderTest = classUnderTest
                        )
                    }
                    .with(anyObjectValueProvider(idGenerator))
                    .withFallback(mockValueProvider.with(AnyDepthNullValueProvider))
                    .with(NullValueProvider)
            )

    override fun handleFuzzedConcreteExecutionResult(
        methodUnderTest: ExecutableId,
        concreteExecutionResult: UtConcreteExecutionResult
    ) {
        delegateContext.handleFuzzedConcreteExecutionResult(methodUnderTest, concreteExecutionResult)
        mockValueProvider.addMockingCandidates(concreteExecutionResult.detectedMockingCandidates)
    }
}