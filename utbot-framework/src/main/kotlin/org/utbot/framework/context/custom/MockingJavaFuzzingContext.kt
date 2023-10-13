package org.utbot.framework.context.custom

import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.providers.AnyDepthNullValueProvider
import org.utbot.fuzzing.providers.AnyObjectValueProvider
import org.utbot.fuzzing.spring.unit.MockValueProvider
import org.utbot.fuzzing.spring.decorators.filterSeeds
import org.utbot.fuzzing.spring.decorators.filterTypes
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

/**
 * Makes fuzzer to use mocks in accordance with [mockPredicate].
 *
 * NOTE:
 *   - fuzzer won't mock types, that have *specific* value providers
 *   (i.e. ones that do not implement [AnyObjectValueProvider])
 *   - fuzzer may still resort to mocks despite [mockPredicate] and *specific*
 *   value providers if it can't create other non-null values or at runtime
 */
fun JavaFuzzingContext.useMocks(mockPredicate: (FuzzedType) -> Boolean) =
    MockingJavaFuzzingContext(delegateContext = this, mockPredicate)

class MockingJavaFuzzingContext(
    val delegateContext: JavaFuzzingContext,
    val mockPredicate: (FuzzedType) -> Boolean,
) : JavaFuzzingContext by delegateContext {
    private val mockValueProvider = MockValueProvider(delegateContext.idGenerator)

    override val valueProvider: JavaValueProvider =

        delegateContext.valueProvider
            // NOTE: we first remove `AnyObjectValueProvider` and `NullValueProvider` from `delegateContext.valueProvider`
            //       and then add them back as a part of our `withFallback` so they have the same priority as
            //       `mockValueProvider`, otherwise mocks will never be used where `null` or new object can be used.
            .except { it is AnyObjectValueProvider }
            .withFallback(
                mockValueProvider.filterTypes(mockPredicate)
                    .with(
                        delegateContext.valueProvider
                            .filterTypes { !mockPredicate(it) }
                            .filterSeeds { (it as? Seed.Simple)?.value?.model !is UtNullModel  }
                    )
                    .withFallback(mockValueProvider.with(AnyDepthNullValueProvider))
            )

    override fun handleFuzzedConcreteExecutionResult(
        methodUnderTest: ExecutableId,
        concreteExecutionResult: UtConcreteExecutionResult
    ) {
        delegateContext.handleFuzzedConcreteExecutionResult(methodUnderTest, concreteExecutionResult)
        mockValueProvider.addMockingCandidates(concreteExecutionResult.detectedMockingCandidates)
    }
}