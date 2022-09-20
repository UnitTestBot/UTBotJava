package org.utbot.examples.mock.model

import org.utbot.examples.mock.provider.impl.ProviderImpl
import org.utbot.examples.mock.service.impl.ServiceWithField
import org.utbot.tests.infrastructure.primitiveValue
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.isNotNull
import org.utbot.framework.plugin.api.isNull
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtModelTestCaseChecker
import org.utbot.testcheckers.eq

internal class FieldMockChecker : UtModelTestCaseChecker(testClass = ServiceWithField::class) {
    @Test
    fun testMockForField_IntPrimitive() {
        checkStatic(
            ServiceWithField::staticCalculateBasedOnInteger,
            eq(4),
            { service, r -> service.isNull() && r.isException<NullPointerException>() },
            { service, r -> service.provider.isNull() && r.isException<NullPointerException>() },
            { service, r ->
                service.provider.isNotNull() &&
                        service.provider.mocksMethod(ProviderImpl::provideInteger)!!.single()
                            .primitiveValue<Int>() > 5 && r.primitiveValue<Int>() == 1
            },
            { service, r ->
                service.provider.isNotNull() &&
                        service.provider.mocksMethod(ProviderImpl::provideInteger)!!.single()
                            .primitiveValue<Int>() <= 5 && r.primitiveValue<Int>() == 0
            },
            mockStrategy = OTHER_PACKAGES
        )
    }

    private val UtModel.provider: UtModel
        get() = this.findField("provider")
}